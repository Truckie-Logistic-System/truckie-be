package capstone_project.config.expired;

import capstone_project.common.enums.ContractStatusEnum;
import capstone_project.common.enums.OrderDetailStatusEnum;
import capstone_project.common.enums.OrderStatusEnum;
import capstone_project.entity.order.contract.ContractEntity;
import capstone_project.entity.order.order.OrderDetailEntity;
import capstone_project.entity.order.order.OrderEntity;
import capstone_project.repository.entityServices.order.contract.ContractEntityService;
import capstone_project.repository.entityServices.order.order.OrderDetailEntityService;
import capstone_project.repository.entityServices.order.order.OrderEntityService;
import capstone_project.service.services.order.order.OrderService;
import capstone_project.service.services.order.order.OrderCancellationContext;
import capstone_project.service.services.order.order.OrderStatusWebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Background scheduler to check and handle contract expiration scenarios
 * 
 * Checks for:
 * 1. Expired signing deadline - Contracts in CONTRACT_DRAFT status that haven't been signed within 24 hours
 * 2. Expired deposit payment deadline - Contracts in CONTRACT_SIGNED status without deposit payment within 24 hours after signing
 * 3. Expired full payment deadline - Contracts in DEPOSITED status without full payment before 1 day prior to pickup time
 * 
 * When deadlines are missed, the order and contract are automatically cancelled
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContractExpiryScheduler {
    private final ContractEntityService contractEntityService;
    private final OrderEntityService orderEntityService;
    private final OrderDetailEntityService orderDetailEntityService;
    private final OrderStatusWebSocketService orderStatusWebSocketService;
    private final OrderService orderService;

    /**
     * Check for expired contracts every 10 minutes
     * Reasonable deadlines:
     * - Contract signing: 24 hours after contract draft creation
     * - Deposit payment: 24 hours after contract signing
     * - Full payment: 1 day before pickup time (earliest estimated start time)
     */
    @Scheduled(fixedRate = 600000) // Runs every 10 minutes
    public void checkExpiredContracts() {

        LocalDateTime now = LocalDateTime.now();
        
        // Check expired signing deadlines
        checkExpiredSigningDeadlines(now);
        
        // Check expired deposit payment deadlines
        checkExpiredDepositPaymentDeadlines(now);
        
        // Check expired full payment deadlines
        checkExpiredFullPaymentDeadlines(now);

    }

    /**
     * Check for contracts in CONTRACT_DRAFT status with expired signing deadline
     * Deadline: 24 hours after contract creation
     */
    private void checkExpiredSigningDeadlines(LocalDateTime now) {
        try {
            List<ContractEntity> expiredContracts = contractEntityService
                    .findByStatusAndSigningDeadlineBefore(
                            ContractStatusEnum.CONTRACT_DRAFT.name(), 
                            now
                    );

            if (expiredContracts.isEmpty()) {
                
                return;
            }

            for (ContractEntity contract : expiredContracts) {
                try {
                    cancelOrderAndContract(
                            contract, 
                            "Hợp đồng hết hạn ký - đã quá 24 giờ kể từ khi tạo hợp đồng",
                            "signing deadline",
                            List.of(OrderStatusEnum.CONTRACT_DRAFT.name())
                    );
                } catch (Exception e) {
                    log.error("Failed to cancel order for contract {} due to expired signing deadline: {}", 
                            contract.getId(), e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            log.error("Error checking expired signing deadlines: {}", e.getMessage(), e);
        }
    }

    /**
     * Check for contracts in CONTRACT_SIGNED status with expired deposit payment deadline
     * Deadline: 24 hours after contract signing
     */
    private void checkExpiredDepositPaymentDeadlines(LocalDateTime now) {
        try {
            List<ContractEntity> expiredContracts = contractEntityService
                    .findByStatusAndDepositPaymentDeadlineBefore(
                            ContractStatusEnum.CONTRACT_SIGNED.name(), 
                            now
                    );

            if (expiredContracts.isEmpty()) {
                
                return;
            }

            for (ContractEntity contract : expiredContracts) {
                try {
                    cancelOrderAndContract(
                            contract, 
                            "Hợp đồng hết hạn thanh toán cọc - đã quá 24 giờ kể từ khi ký hợp đồng",
                            "deposit payment deadline",
                            List.of(OrderStatusEnum.CONTRACT_SIGNED.name())
                    );
                } catch (Exception e) {
                    log.error("Failed to cancel order for contract {} due to expired deposit payment deadline: {}", 
                            contract.getId(), e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            log.error("Error checking expired deposit payment deadlines: {}", e.getMessage(), e);
        }
    }

    /**
     * Check for contracts in DEPOSITED status with expired full payment deadline
     * Deadline: Before pickup time (estimate start time)
     */
    private void checkExpiredFullPaymentDeadlines(LocalDateTime now) {
        try {
            List<ContractEntity> expiredContracts = contractEntityService
                    .findByStatusAndFullPaymentDeadlineBefore(
                            ContractStatusEnum.DEPOSITED.name(), 
                            now
                    );

            if (expiredContracts.isEmpty()) {
                
                return;
            }

            log.info("Found {} contracts with expired full payment deadlines", expiredContracts.size());

            for (ContractEntity contract : expiredContracts) {
                try {
                    cancelOrderAndContract(
                            contract, 
                            "Hợp đồng hết hạn thanh toán toàn bộ - đã quá thời gian lấy hàng",
                            "full payment deadline",
                            List.of(OrderStatusEnum.ASSIGNED_TO_DRIVER.name())
                    );
                } catch (Exception e) {
                    log.error("Failed to cancel order for contract {} due to expired full payment deadline: {}", 
                            contract.getId(), e.getMessage(), e);
                    // Continue processing other contracts even if one fails
                }
            }
        } catch (Exception e) {
            log.error("Error checking expired full payment deadlines: {}", e.getMessage(), e);
        }
    }

    /**
     * Cancel both order and contract due to expired deadline using unified cancellation
     */
    private void cancelOrderAndContract(
            ContractEntity contract,
            String reason,
            String deadlineType,
            List<String> expectedOrderStatuses
    ) {
        // Get the associated orderId (safe even if orderEntity is a Hibernate proxy)
        OrderEntity contractOrder = contract.getOrderEntity();
        if (contractOrder == null || contractOrder.getId() == null) {
            log.warn("Contract {} has no associated order", contract.getId());
            return;
        }

        // Load order explicitly to avoid LazyInitializationException (scheduler may run without open session)
        OrderEntity order = orderEntityService.findEntityById(contractOrder.getId())
                .orElse(null);
        if (order == null) {
            log.warn("Contract {} references missing order {}", contract.getId(), contractOrder.getId());
            return;
        }
        
        // Skip orders that have already been paid and moved to ON_PLANNING or later stages
        // This indicates the payment was completed but contract status wasn't updated
        String currentStatus = order.getStatus();
        List<String> paidStatuses = List.of(
                OrderStatusEnum.ON_PLANNING.name(),
                OrderStatusEnum.FULLY_PAID.name(),
                OrderStatusEnum.PICKING_UP.name(),
                OrderStatusEnum.ON_DELIVERED.name(),
                OrderStatusEnum.ONGOING_DELIVERED.name(),
                OrderStatusEnum.IN_TROUBLES.name(),
                OrderStatusEnum.DELIVERED.name(),
                OrderStatusEnum.SUCCESSFUL.name()
        );
        
        if (paidStatuses.contains(currentStatus)) {
            log.warn("⚠️ Skipping cancellation for order {} (contract {}). Order is in status {} which indicates payment was completed. Contract status may need manual update.", 
                    order.getOrderCode(), contract.getId(), currentStatus);
            return;
        }

        if (expectedOrderStatuses != null && !expectedOrderStatuses.isEmpty() && !expectedOrderStatuses.contains(currentStatus)) {
            log.warn(
                    "⚠️ Skipping cancellation for order {} (contract {}). Deadline type {} triggered but order status is {} (expected {}).",
                    order.getOrderCode(),
                    contract.getId(),
                    deadlineType,
                    currentStatus,
                    expectedOrderStatuses
            );
            return;
        }

        // Create context for contract expiry cancellation
        OrderCancellationContext context = OrderCancellationContext.builder()
                .cancellationType(OrderCancellationContext.CancellationType.CONTRACT_EXPIRY)
                .customReason(reason)
                .sendNotifications(true)
                .cleanupReservations(true)
                .build();

        // Use unified cancellation for atomic transaction
        try {
            orderService.cancelOrderUnified(order.getId(), context);
            log.info("✅ Used unified cancellation to cancel order {} due to {}", order.getOrderCode(), deadlineType);
        } catch (Exception e) {
            log.error("❌ Failed to use unified cancellation for order {}: {}", order.getId(), e.getMessage(), e);
            // Fallback to manual cancellation if unified method fails
            fallbackManualCancellation(contract, order, reason);
        }
    }

    /**
     * Fallback manual cancellation if unified method fails
     * This preserves the original logic as backup
     */
    private void fallbackManualCancellation(ContractEntity contract, OrderEntity order, String reason) {
        log.warn("⚠️ Using fallback manual cancellation for order {} due to contract expiry", order.getOrderCode());
        
        // Only cancel if order is in a cancellable state
        String currentStatus = order.getStatus();
        List<String> cancellableStatuses = List.of(
                OrderStatusEnum.PENDING.name(),
                OrderStatusEnum.PROCESSING.name(),
                OrderStatusEnum.CONTRACT_DRAFT.name(),
                OrderStatusEnum.CONTRACT_SIGNED.name(),
                OrderStatusEnum.ASSIGNED_TO_DRIVER.name()
        );
        
        // Check if order has already progressed past cancellable stages
        if (!cancellableStatuses.contains(currentStatus)) {
            log.warn("Order {} is in status {} and cannot be automatically cancelled. Updating contract to EXPIRED only.", 
                    order.getOrderCode(), currentStatus);
            // Still update contract status to EXPIRED for record keeping
            contract.setStatus(ContractStatusEnum.EXPIRED.name());
            contractEntityService.save(contract);
            return;
        }
        
        // Update contract status to EXPIRED
        contract.setStatus(ContractStatusEnum.EXPIRED.name());
        contractEntityService.save(contract);
        
        // Proceed with order cancellation
        try {
            OrderStatusEnum previousStatus = OrderStatusEnum.valueOf(currentStatus);
            order.setStatus(OrderStatusEnum.CANCELLED.name());
            order.setCancellationReason(reason);
            orderEntityService.save(order);
            
            // Cancel all order details
            List<OrderDetailEntity> orderDetails = order.getOrderDetailEntities();
            if (orderDetails != null && !orderDetails.isEmpty()) {
                for (OrderDetailEntity orderDetail : orderDetails) {
                    orderDetail.setStatus(OrderDetailStatusEnum.CANCELLED.name());
                }
                orderDetailEntityService.saveAllOrderDetailEntities(orderDetails);
                log.info("✅ Cancelled {} order details for order {}", orderDetails.size(), order.getOrderCode());
            }

            // Send WebSocket notification to customer
            orderStatusWebSocketService.sendOrderStatusChange(
                    order.getId(),
                    order.getOrderCode(),
                    previousStatus,
                    OrderStatusEnum.CANCELLED
            );
            
            log.info("✅ Successfully cancelled order {} using fallback method", order.getOrderCode());
        } catch (Exception e) {
            log.error("❌ Failed to complete fallback cancellation for order {}: {}", order.getOrderCode(), e.getMessage());
        }
    }
}
