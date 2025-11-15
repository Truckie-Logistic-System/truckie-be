package capstone_project.service.services.issue.impl;

import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.enums.IssueEnum;
import capstone_project.common.enums.IssueCategoryEnum;
import capstone_project.common.enums.TransactionEnum;
import capstone_project.common.exceptions.dto.BadRequestException;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.response.issue.OrderRejectionDetailResponse;
import capstone_project.entity.issue.IssueEntity;
import capstone_project.entity.order.order.OrderEntity;
import capstone_project.entity.order.order.OrderDetailEntity;
import capstone_project.entity.order.transaction.TransactionEntity;
import capstone_project.repository.entityServices.issue.IssueEntityService;
import capstone_project.repository.entityServices.order.order.OrderEntityService;
import capstone_project.repository.entityServices.order.order.OrderDetailEntityService;
import capstone_project.repository.entityServices.order.transaction.TransactionEntityService;
import capstone_project.repository.entityServices.user.CustomerEntityService;
import capstone_project.service.services.issue.CustomerIssueService;
import capstone_project.service.services.issue.IssueService;
import capstone_project.service.services.websocket.IssueWebSocketService;
import capstone_project.common.utils.UserContextUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of customer-specific issue operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerIssueServiceImpl implements CustomerIssueService {
    
    private final IssueEntityService issueEntityService;
    private final OrderEntityService orderEntityService;
    private final OrderDetailEntityService orderDetailEntityService;
    private final CustomerEntityService customerEntityService;
    private final TransactionEntityService transactionEntityService;
    private final IssueService issueService;
    private final IssueWebSocketService issueWebSocketService;
    private final UserContextUtils userContextUtils;
    
    @Override
    public List<OrderRejectionDetailResponse> getCustomerOrderRejectionIssues() {
        log.info("Getting ORDER_REJECTION issues for current customer");
        
        // Get current customer ID from security context
        UUID customerId = getCurrentCustomerId();
        
        // Get all orders for this customer
        List<OrderEntity> customerOrders = orderEntityService.findAll().stream()
                .filter(order -> order.getSender() != null 
                        && order.getSender().getId().equals(customerId))
                .collect(Collectors.toList());
        
        if (customerOrders.isEmpty()) {
            log.info("No orders found for customer {}", customerId);
            return List.of();
        }
        
        // Get all ORDER_REJECTION issues for these orders
        List<UUID> orderIds = customerOrders.stream()
                .map(OrderEntity::getId)
                .collect(Collectors.toList());
        
        List<IssueEntity> orderRejectionIssues = issueEntityService.findAll().stream()
                .filter(issue -> issue.getIssueTypeEntity() != null
                        && IssueCategoryEnum.ORDER_REJECTION.name().equals(
                                issue.getIssueTypeEntity().getIssueCategory()))
                .filter(issue -> {
                    if (issue.getVehicleAssignmentEntity() == null) return false;
                    // Get order from vehicle assignment via order details
                    List<OrderDetailEntity> orderDetails = orderDetailEntityService
                            .findByVehicleAssignmentEntity(issue.getVehicleAssignmentEntity());
                    if (orderDetails.isEmpty()) return false;
                    UUID orderId = orderDetails.get(0).getOrderEntity().getId();
                    return orderIds.contains(orderId);
                })
                .filter(issue -> IssueEnum.IN_PROGRESS.name().equals(issue.getStatus())
                        || IssueEnum.RESOLVED.name().equals(issue.getStatus()))
                .collect(Collectors.toList());
        
        log.info("Found {} ORDER_REJECTION issues for customer {}", 
                orderRejectionIssues.size(), customerId);
        
        // Convert to response DTOs
        return orderRejectionIssues.stream()
                .map(issue -> {
                    try {
                        return issueService.getOrderRejectionDetail(issue.getId());
                    } catch (Exception e) {
                        log.error("Error getting detail for issue {}: {}", 
                                issue.getId(), e.getMessage());
                        return null;
                    }
                })
                .filter(response -> response != null)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<OrderRejectionDetailResponse> getOrderRejectionIssuesByOrder(UUID orderId) {
        log.info("Getting ORDER_REJECTION issues for order {}", orderId);
        
        // Verify order belongs to current customer
        UUID customerId = getCurrentCustomerId();
        OrderEntity order = orderEntityService.findEntityById(orderId)
                .orElseThrow(() -> new NotFoundException(ErrorEnum.NOT_FOUND));
        
        if (order.getSender() == null || !order.getSender().getId().equals(customerId)) {
            log.warn("Customer {} trying to access order {} that doesn't belong to them", 
                    customerId, orderId);
            throw new BadRequestException(ErrorEnum.UNAUTHORIZED);
        }
        
        // Get ORDER_REJECTION issues for this order
        List<IssueEntity> orderRejectionIssues = issueEntityService.findAll().stream()
                .filter(issue -> issue.getIssueTypeEntity() != null
                        && IssueCategoryEnum.ORDER_REJECTION.name().equals(
                                issue.getIssueTypeEntity().getIssueCategory()))
                .filter(issue -> {
                    if (issue.getVehicleAssignmentEntity() == null) return false;
                    // Get order from vehicle assignment via order details
                    List<OrderDetailEntity> orderDetails = orderDetailEntityService
                            .findByVehicleAssignmentEntity(issue.getVehicleAssignmentEntity());
                    if (orderDetails.isEmpty()) return false;
                    return orderDetails.get(0).getOrderEntity().getId().equals(orderId);
                })
                .filter(issue -> IssueEnum.IN_PROGRESS.name().equals(issue.getStatus())
                        || IssueEnum.RESOLVED.name().equals(issue.getStatus()))
                .collect(Collectors.toList());
        
        log.info("Found {} ORDER_REJECTION issues for order {}", 
                orderRejectionIssues.size(), orderId);
        
        // Convert to response DTOs
        return orderRejectionIssues.stream()
                .map(issue -> {
                    try {
                        return issueService.getOrderRejectionDetail(issue.getId());
                    } catch (Exception e) {
                        log.error("Error getting detail for issue {}: {}", 
                                issue.getId(), e.getMessage());
                        return null;
                    }
                })
                .filter(response -> response != null)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public capstone_project.dtos.response.order.transaction.TransactionResponse createReturnPaymentTransaction(UUID issueId) {
        log.info("💳 Customer creating return payment for issue {}", issueId);
        
        // Verify issue belongs to current customer
        UUID customerId = getCurrentCustomerId();
        IssueEntity issue = issueEntityService.findEntityById(issueId)
                .orElseThrow(() -> new NotFoundException(ErrorEnum.ISSUE_NOT_FOUND));
        
        // Verify issue type
        if (issue.getIssueTypeEntity() == null 
                || !IssueCategoryEnum.ORDER_REJECTION.name().equals(
                        issue.getIssueTypeEntity().getIssueCategory())) {
            throw new BadRequestException(ErrorEnum.INVALID);
        }
        
        // Verify issue belongs to current customer
        if (issue.getVehicleAssignmentEntity() == null) {
            throw new BadRequestException(ErrorEnum.INVALID);
        }
        
        // Get order from vehicle assignment
        List<OrderDetailEntity> orderDetails = orderDetailEntityService
                .findByVehicleAssignmentEntity(issue.getVehicleAssignmentEntity());
        if (orderDetails.isEmpty()) {
            throw new BadRequestException(ErrorEnum.INVALID);
        }
        
        OrderEntity order = orderDetails.get(0).getOrderEntity();
        if (order == null || order.getSender() == null 
                || !order.getSender().getId().equals(customerId)) {
            log.warn("Customer {} trying to create payment for issue {} that doesn't belong to them", 
                    customerId, issueId);
            throw new BadRequestException(ErrorEnum.UNAUTHORIZED);
        }
        
        // Delegate to IssueService to create payment transaction
        return issueService.createReturnPaymentTransaction(issueId);
    }
    
    @Override
    @Transactional
    public void rejectReturnPayment(UUID issueId) {
        log.info("Customer rejecting return payment for issue {}", issueId);
        
        // Get issue
        IssueEntity issue = issueEntityService.findEntityById(issueId)
                .orElseThrow(() -> new NotFoundException(ErrorEnum.ISSUE_NOT_FOUND));
        
        // Verify issue type
        if (issue.getIssueTypeEntity() == null 
                || !IssueCategoryEnum.ORDER_REJECTION.name().equals(
                        issue.getIssueTypeEntity().getIssueCategory())) {
            throw new BadRequestException(ErrorEnum.INVALID);
        }
        
        // Verify issue belongs to current customer
        UUID customerId = getCurrentCustomerId();
        if (issue.getVehicleAssignmentEntity() == null) {
            throw new BadRequestException(ErrorEnum.INVALID);
        }
        
        // Get order from vehicle assignment
        List<OrderDetailEntity> orderDetails = orderDetailEntityService
                .findByVehicleAssignmentEntity(issue.getVehicleAssignmentEntity());
        if (orderDetails.isEmpty()) {
            throw new BadRequestException(ErrorEnum.INVALID);
        }
        
        OrderEntity order = orderDetails.get(0).getOrderEntity();
        if (order == null || order.getSender() == null 
                || !order.getSender().getId().equals(customerId)) {
            log.warn("Customer {} trying to reject payment for issue {} that doesn't belong to them", 
                    customerId, issueId);
            throw new BadRequestException(ErrorEnum.UNAUTHORIZED);
        }
        
        // Verify issue status is IN_PROGRESS
        if (!IssueEnum.IN_PROGRESS.name().equals(issue.getStatus())) {
            throw new BadRequestException(ErrorEnum.INVALID);
        }
        
        // Verify transaction exists and is PENDING
        if (issue.getReturnTransaction() == null) {
            throw new BadRequestException(ErrorEnum.INVALID);
        }
        
        TransactionEntity transaction = issue.getReturnTransaction();
        if (!TransactionEnum.PENDING.name().equals(transaction.getStatus())) {
            throw new BadRequestException(ErrorEnum.INVALID);
        }
        
        // Update transaction status to CANCELLED
        transaction.setStatus(TransactionEnum.CANCELLED.name());
        transactionEntityService.save(transaction);
        
        // Update issue status to RESOLVED (payment rejected)
        issue.setStatus(IssueEnum.RESOLVED.name());
        issue.setResolvedAt(LocalDateTime.now());
        issueEntityService.save(issue);
        
        // Journey remains INACTIVE (will not be activated)
        if (issue.getReturnJourney() != null) {
            log.info("Journey {} remains INACTIVE due to payment rejection", 
                    issue.getReturnJourney().getId());
        }
        
        // Send notification to driver
        try {
            var vehicleAssignment = issue.getVehicleAssignmentEntity();
            if (vehicleAssignment != null && vehicleAssignment.getDriver1() != null) {
                UUID driverId = vehicleAssignment.getDriver1().getUser().getId();
                
                // Create notification payload
                var notification = new java.util.HashMap<String, Object>();
                notification.put("type", "RETURN_PAYMENT_REJECTED");
                notification.put("priority", "HIGH");
                notification.put("title", "Khách hàng từ chối thanh toán");
                notification.put("message", "Khách hàng đã từ chối thanh toán cước trả hàng. " +
                        "Vui lòng tiếp tục theo lộ trình ban đầu về carrier. " +
                        "Các kiện hàng bị từ chối sẽ được hủy.");
                notification.put("issueId", issueId.toString());
                notification.put("vehicleAssignmentId", vehicleAssignment.getId().toString());
                notification.put("timestamp", java.time.Instant.now().toString());
                
                // Send via WebSocket
                issueWebSocketService.sendReturnPaymentTimeoutNotification(
                        driverId,
                        issueId,
                        vehicleAssignment.getId()
                );
                
                log.info("Sent payment rejection notification to driver: {}", driverId);
            }
        } catch (Exception e) {
            log.error("Failed to send rejection notification: {}", e.getMessage(), e);
            // Don't throw - notification failure shouldn't break the rejection
        }
        
        log.info("✅ Customer rejected return payment for issue {}", issueId);
    }
    
    /**
     * Get current customer ID from security context
     */
    private UUID getCurrentCustomerId() {
        return userContextUtils.getCurrentCustomerId();
    }
}
