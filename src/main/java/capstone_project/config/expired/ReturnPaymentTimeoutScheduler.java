package capstone_project.config.expired;

import capstone_project.common.enums.IssueEnum;
import capstone_project.common.enums.IssueCategoryEnum;
import capstone_project.common.enums.TransactionEnum;
import capstone_project.entity.issue.IssueEntity;
import capstone_project.repository.entityServices.issue.IssueEntityService;
import capstone_project.repository.entityServices.order.transaction.TransactionEntityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled task to check and handle expired return shipping payments
 * For ORDER_REJECTION issues with 24-hour payment deadline
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReturnPaymentTimeoutScheduler {
    
    private final IssueEntityService issueEntityService;
    private final TransactionEntityService transactionEntityService;
    private final capstone_project.repository.entityServices.order.order.OrderDetailEntityService orderDetailEntityService;
    private final capstone_project.service.services.websocket.IssueWebSocketService issueWebSocketService;
    private final capstone_project.service.services.order.order.OrderDetailStatusWebSocketService orderDetailStatusWebSocketService;

    /**
     * Check for expired return payment deadlines every 30 minutes
     * Auto-cancel transactions and notify drivers when deadline is exceeded
     */
    @Scheduled(cron = "0 */30 * * * *") // Every 30 minutes
    public void checkReturnPaymentDeadlines() {
        log.info("🕐 [ReturnPaymentTimeoutScheduler] Starting return payment deadline check...");
        
        try {
            // Find all ORDER_REJECTION issues that are IN_PROGRESS
            List<IssueEntity> inProgressIssues = issueEntityService.findAll().stream()
                    .filter(issue -> IssueEnum.IN_PROGRESS.name().equals(issue.getStatus()))
                    .filter(issue -> issue.getIssueTypeEntity() != null 
                            && IssueCategoryEnum.ORDER_REJECTION.name().equals(
                                    issue.getIssueTypeEntity().getIssueCategory()))
                    .toList();
            
            log.info("📋 Found {} ORDER_REJECTION issues in IN_PROGRESS status", inProgressIssues.size());
            
            LocalDateTime now = LocalDateTime.now();
            int expiredCount = 0;
            
            for (IssueEntity issue : inProgressIssues) {
                // Check if payment deadline has passed
                if (issue.getPaymentDeadline() != null 
                        && issue.getPaymentDeadline().isBefore(now)) {
                    
                    // Find transaction by issueId
                    var transactionOpt = transactionEntityService.findAll().stream()
                            .filter(tx -> issue.getId().equals(tx.getIssueId()))
                            .findFirst();
                    
                    // Check if transaction is still PENDING
                    if (transactionOpt.isPresent() 
                            && TransactionEnum.PENDING.name().equals(transactionOpt.get().getStatus())) {
                        
                        log.warn("⏰ Issue {} payment deadline expired at {}", 
                                issue.getId(), issue.getPaymentDeadline());
                        
                        // Update transaction status to EXPIRED
                        var transaction = transactionOpt.get();
                        transaction.setStatus(TransactionEnum.EXPIRED.name());
                        transactionEntityService.save(transaction);
                        
                        // Update issue status to RESOLVED (payment rejected by timeout)
                        issue.setStatus(IssueEnum.RESOLVED.name());
                        issue.setResolvedAt(LocalDateTime.now());
                        issueEntityService.save(issue);
                        
                        // Update order details status to CANCELLED (customer didn't pay, packages are abandoned)
                        if (issue.getOrderDetails() != null && !issue.getOrderDetails().isEmpty()) {
                            java.util.UUID vehicleAssignmentId = issue.getVehicleAssignmentEntity() != null ? 
                                    issue.getVehicleAssignmentEntity().getId() : null;
                            
                            issue.getOrderDetails().forEach(orderDetail -> {
                                String oldStatus = orderDetail.getStatus();
                                orderDetail.setStatus(capstone_project.common.enums.OrderDetailStatusEnum.CANCELLED.name());
                                orderDetailEntityService.save(orderDetail);
                                
                                // Send WebSocket notification
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
                                            capstone_project.common.enums.OrderDetailStatusEnum.CANCELLED
                                        );
                                    } catch (Exception e) {
                                        log.error("❌ Failed to send WebSocket for {}: {}", 
                                                orderDetail.getTrackingCode(), e.getMessage());
                                    }
                                }
                                
                                log.info("📦 OrderDetail {} status updated to CANCELLED (payment timeout)", 
                                        orderDetail.getTrackingCode());
                            });
                            log.info("✅ Updated {} order details to CANCELLED status", issue.getOrderDetails().size());
                        }
                        
                        // Journey remains INACTIVE (will not be activated)
                        if (issue.getReturnJourney() != null) {
                            log.info("🚫 Journey {} remains INACTIVE due to payment timeout", 
                                    issue.getReturnJourney().getId());
                        }
                        
                        // Send notification to driver
                        sendTimeoutNotificationToDriver(issue);
                        
                        expiredCount++;
                    }
                }
            }
            
            if (expiredCount > 0) {
                log.info("✅ Processed {} expired return payments", expiredCount);
            } else {
                log.info("✅ No expired return payments found");
            }
            
        } catch (Exception e) {
            log.error("❌ Error checking return payment deadlines: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Send notification to driver that customer payment expired
     * Driver should continue with original route, items will be cancelled
     */
    private void sendTimeoutNotificationToDriver(IssueEntity issue) {
        try {
            var vehicleAssignment = issue.getVehicleAssignmentEntity();
            if (vehicleAssignment == null || vehicleAssignment.getDriver1() == null) {
                log.warn("No driver found for issue {}, skipping notification", issue.getId());
                return;
            }
            
            var driver = vehicleAssignment.getDriver1();
            var driverId = driver.getUser().getId();
            
            // Use existing WebSocket service to send notification
            issueWebSocketService.sendReturnPaymentTimeoutNotification(
                driverId,
                issue.getId(),
                vehicleAssignment.getId()
            );
            
            log.info("📢 Sent payment timeout notification to driver: {}", driverId);
            
        } catch (Exception e) {
            log.error("❌ Failed to send timeout notification: {}", e.getMessage(), e);
            // Don't throw - notification failure shouldn't break the scheduler
        }
    }
}
