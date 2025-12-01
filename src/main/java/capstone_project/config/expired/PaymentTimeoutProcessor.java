package capstone_project.config.expired;

import capstone_project.common.enums.IssueEnum;
import capstone_project.common.enums.OrderDetailStatusEnum;
import capstone_project.common.enums.OrderStatusEnum;
import capstone_project.common.enums.TransactionEnum;
import capstone_project.entity.issue.IssueEntity;
import capstone_project.entity.order.order.OrderEntity;
import capstone_project.repository.entityServices.issue.IssueEntityService;
import capstone_project.repository.entityServices.order.order.JourneyHistoryEntityService;
import capstone_project.repository.entityServices.order.order.OrderDetailEntityService;
import capstone_project.repository.entityServices.order.order.OrderEntityService;
import capstone_project.repository.entityServices.order.transaction.TransactionEntityService;
import capstone_project.service.services.order.order.OrderDetailStatusService;
import capstone_project.service.services.order.order.OrderDetailStatusWebSocketService;
import capstone_project.service.services.websocket.IssueWebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service to handle payment timeout processing within a transaction
 * This ensures all database operations happen within a single transaction context
 * and prevents LazyInitializationException
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentTimeoutProcessor {
    
    private final IssueEntityService issueEntityService;
    private final TransactionEntityService transactionEntityService;
    private final OrderDetailEntityService orderDetailEntityService;
    private final OrderEntityService orderEntityService;
    private final IssueWebSocketService issueWebSocketService;
    private final OrderDetailStatusWebSocketService orderDetailStatusWebSocketService;
    private final JourneyHistoryEntityService journeyHistoryEntityService;
    private final OrderDetailStatusService orderDetailStatusService;
    
    /**
     * Process payment timeout for an issue
     * This method runs within a transaction to prevent LazyInitializationException
     * 
     * @param issueId The issue ID to process timeout for
     * @return true if processed successfully, false if skipped (already resolved, etc.)
     */
    @Transactional
    public boolean processTimeout(UUID issueId) {
        
        // Find issue with orderDetails eagerly fetched
        var issueOpt = issueEntityService.findByIdWithDetails(issueId);
        if (issueOpt.isEmpty()) {
            log.warn("Issue {} not found, skipping timeout processing", issueId);
            return false;
        }
        
        IssueEntity issue = issueOpt.get();
        
        // Only process if still IN_PROGRESS (not paid yet)
        if (!IssueEnum.IN_PROGRESS.name().equals(issue.getStatus())) {
            return false;
        }
        
        LocalDateTime now = LocalDateTime.now();
        
        // Verify deadline has actually passed (with 2s buffer for timing)
        if (issue.getPaymentDeadline() == null || issue.getPaymentDeadline().isAfter(now.minusSeconds(2))) {
            return false;
        }
        
        log.warn("⏰ Issue {} payment deadline expired at {}", issueId, issue.getPaymentDeadline());
        
        // ═══════════════════════════════════════════════════════════════
        // STEP 1: Update Transaction to EXPIRED (if exists)
        // ═══════════════════════════════════════════════════════════════
        var transactionOpt = transactionEntityService.findAll().stream()
                .filter(tx -> issue.getId().equals(tx.getIssueId()))
                .findFirst();
        
        if (transactionOpt.isPresent()) {
            var transaction = transactionOpt.get();
            if (TransactionEnum.PENDING.name().equals(transaction.getStatus())) {
                transaction.setStatus(TransactionEnum.EXPIRED.name());
                transactionEntityService.save(transaction);
            }
        } else {
        }
        
        // ═══════════════════════════════════════════════════════════════
        // STEP 2: Update Issue status to RESOLVED
        // ═══════════════════════════════════════════════════════════════
        issue.setStatus(IssueEnum.RESOLVED.name());
        issue.setResolvedAt(now);
        issueEntityService.save(issue);
        
        // ═══════════════════════════════════════════════════════════════
        // STEP 3: Cancel rejected OrderDetails
        // ═══════════════════════════════════════════════════════════════
        if (issue.getOrderDetails() != null && !issue.getOrderDetails().isEmpty()) {
            UUID vehicleAssignmentId = issue.getVehicleAssignmentEntity() != null ? 
                    issue.getVehicleAssignmentEntity().getId() : null;
            
            issue.getOrderDetails().forEach(orderDetail -> {
                String oldStatus = orderDetail.getStatus();
                orderDetail.setStatus(OrderDetailStatusEnum.CANCELLED.name());
                orderDetailEntityService.save(orderDetail);
                
                // Send WebSocket notification for OrderDetail status change
                var order = orderDetail.getOrderEntity();
                if (order != null) {
                    try {
                        orderDetailStatusWebSocketService.sendOrderDetailStatusChange(
                            orderDetail.getId(),
                            orderDetail.getTrackingCode(),
                            order.getId(),
                            order.getOrderCode(),
                            vehicleAssignmentId,
                            oldStatus,
                            OrderDetailStatusEnum.CANCELLED
                        );
                    } catch (Exception e) {
                        log.error("❌ Failed to send WebSocket for {}: {}", 
                                orderDetail.getTrackingCode(), e.getMessage());
                    }
                }
                
            });
            
            // ═══════════════════════════════════════════════════════════════
            // STEP 4: Auto-update Order status based on ALL OrderDetails
            // ═══════════════════════════════════════════════════════════════
            if (!issue.getOrderDetails().isEmpty()) {
                try {
                    UUID orderId = issue.getOrderDetails().get(0).getOrderEntity().getId();
                    orderDetailStatusService.triggerOrderStatusUpdate(orderId);
                    
                    // Set cancellation reason for return payment timeout
                    setOrderCancellationReason(orderId, "Quá hạn thanh toán cước trả hàng - không thanh toán trong thời gian quy định");
                } catch (Exception e) {
                    log.error("❌ [PaymentTimeoutProcessor] Failed to update Order status: {}", e.getMessage(), e);
                }
            }
        }
        
        // ═══════════════════════════════════════════════════════════════
        // STEP 5: Delete inactive return journey
        // ═══════════════════════════════════════════════════════════════
        if (issue.getReturnJourney() != null) {
            try {
                var returnJourney = issue.getReturnJourney();
                journeyHistoryEntityService.delete(returnJourney);
                issue.setReturnJourney(null);
                issueEntityService.save(issue);
            } catch (Exception e) {
                log.error("❌ Failed to delete return journey: {}", e.getMessage());
            }
        }
        
        // ═══════════════════════════════════════════════════════════════
        // STEP 6: Send timeout notifications to all parties
        // ═══════════════════════════════════════════════════════════════
        sendTimeoutNotificationToDriver(issue);
        sendTimeoutNotificationToStaff(issue);
        sendTimeoutNotificationToCustomer(issue);
        
        return true;
    }
    
    private void sendTimeoutNotificationToDriver(IssueEntity issue) {
        try {
            var vehicleAssignment = issue.getVehicleAssignmentEntity();
            if (vehicleAssignment == null) {
                log.warn("⚠️ No vehicle assignment found for issue {}", issue.getId());
                return;
            }
            
            var driver = vehicleAssignment.getDriver1();
            if (driver == null) {
                log.warn("⚠️ No driver1 found for vehicle assignment {}", vehicleAssignment.getId());
                return;
            }
            
            if (driver.getUser() == null) {
                log.warn("⚠️ Driver {} has no user, cannot send notification", driver.getId());
                return;
            }
            
            // CRITICAL FIX: Use driver ID, not user ID (consistent with other notifications)
            var driverId = driver.getId();
            
            
            issueWebSocketService.sendReturnPaymentTimeoutNotification(
                driverId,
                issue.getId(),
                vehicleAssignment.getId()
            );
            
            
        } catch (Exception e) {
            log.error("❌ [PaymentTimeoutProcessor] Failed to send timeout notification to driver: {}", e.getMessage(), e);
        }
    }
    
    private void sendTimeoutNotificationToStaff(IssueEntity issue) {
        try {
            // Get order and customer info
            var order = issue.getOrderDetails() != null && !issue.getOrderDetails().isEmpty() 
                    ? issue.getOrderDetails().get(0).getOrderEntity() 
                    : null;
            
            String customerName = "N/A";
            if (order != null && order.getSender() != null) {
                var sender = order.getSender();
                if (sender.getCompanyName() != null && !sender.getCompanyName().isEmpty()) {
                    customerName = sender.getCompanyName();
                } else if (sender.getRepresentativeName() != null && !sender.getRepresentativeName().isEmpty()) {
                    customerName = sender.getRepresentativeName();
                } else if (sender.getUser() != null) {
                    customerName = sender.getUser().getFullName();
                }
            }
            
            // Get tracking codes
            String trackingCodes = issue.getOrderDetails() != null 
                    ? issue.getOrderDetails().stream()
                        .map(od -> od.getTrackingCode())
                        .collect(java.util.stream.Collectors.joining(", "))
                    : "N/A";
            
            issueWebSocketService.sendReturnPaymentTimeoutNotificationToStaff(
                issue.getId(),
                order != null ? order.getId() : null,
                customerName,
                trackingCodes,
                issue.getVehicleAssignmentEntity() != null 
                    ? issue.getVehicleAssignmentEntity().getTrackingCode() 
                    : "N/A"
            );
            
        } catch (Exception e) {
            log.error("❌ Failed to send timeout notification to staff: {}", e.getMessage(), e);
        }
    }
    
    private void sendTimeoutNotificationToCustomer(IssueEntity issue) {
        try {
            // Get order and customer info
            var order = issue.getOrderDetails() != null && !issue.getOrderDetails().isEmpty() 
                    ? issue.getOrderDetails().get(0).getOrderEntity() 
                    : null;
            
            if (order == null || order.getSender() == null || order.getSender().getUser() == null) {
                log.warn("⚠️ No customer found for issue {}", issue.getId());
                return;
            }
            
            var customerId = order.getSender().getUser().getId();
            
            // Get tracking codes
            String trackingCodes = issue.getOrderDetails() != null 
                    ? issue.getOrderDetails().stream()
                        .map(od -> od.getTrackingCode())
                        .collect(java.util.stream.Collectors.joining(", "))
                    : "N/A";
            
            issueWebSocketService.sendReturnPaymentTimeoutNotificationToCustomer(
                customerId,
                issue.getId(),
                order.getId(),
                trackingCodes
            );
            
        } catch (Exception e) {
            log.error("❌ Failed to send timeout notification to customer: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Set cancellation reason for an order
     * Only sets reason if order is in CANCELLED status
     */
    private void setOrderCancellationReason(UUID orderId, String reason) {
        try {
            OrderEntity order = orderEntityService.findEntityById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
            
            // Only set cancellation reason if order is cancelled
            if (OrderStatusEnum.CANCELLED.name().equals(order.getStatus())) {
                order.setCancellationReason(reason);
                orderEntityService.save(order);
                log.info("✅ Set cancellation reason for order {}: {}", orderId, reason);
            } else {
                log.warn("⚠️ Order {} is not in CANCELLED status (current: {}), cannot set cancellation reason", 
                    orderId, order.getStatus());
            }
        } catch (Exception e) {
            log.error("❌ Failed to set cancellation reason for order {}: {}", orderId, e.getMessage(), e);
        }
    }
}
