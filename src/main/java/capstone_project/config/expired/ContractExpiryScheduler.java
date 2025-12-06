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
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional
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
                            "signing deadline"
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
                            "deposit payment deadline"
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

            for (ContractEntity contract : expiredContracts) {
                try {
                    cancelOrderAndContract(
                            contract, 
                            "Hợp đồng hết hạn thanh toán toàn bộ - đã quá thời gian lấy hàng",
                            "full payment deadline"
                    );
                } catch (Exception e) {
                    log.error("Failed to cancel order for contract {} due to expired full payment deadline: {}", 
                            contract.getId(), e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            log.error("Error checking expired full payment deadlines: {}", e.getMessage(), e);
        }
    }

    /**
     * Cancel both order and contract due to expired deadline using unified cancellation
     */
    private void cancelOrderAndContract(ContractEntity contract, String reason, String deadlineType) {
        // Get the associated order
        OrderEntity order = contract.getOrderEntity();
        if (order == null) {
            log.warn("Contract {} has no associated order", contract.getId());
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
        
        // Update contract status to EXPIRED
        contract.setStatus(ContractStatusEnum.EXPIRED.name());
        contractEntityService.save(contract);

        // Only cancel if order is in a cancellable state
        String currentStatus = order.getStatus();
        List<String> cancellableStatuses = List.of(
                OrderStatusEnum.PENDING.name(),
                OrderStatusEnum.PROCESSING.name(),
                OrderStatusEnum.CONTRACT_DRAFT.name(),
                OrderStatusEnum.CONTRACT_SIGNED.name()
        );

        if (cancellableStatuses.contains(currentStatus)) {
            OrderStatusEnum previousStatus = OrderStatusEnum.valueOf(currentStatus);
            order.setStatus(OrderStatusEnum.CANCELLED.name());
            order.setCancellationReason(reason);
            orderEntityService.save(order);
            
            // Cancel all order details
            try {
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
            } catch (Exception e) {
                log.error("❌ Failed to complete fallback cancellation for order {}: {}", order.getId(), e.getMessage());
            }
        } else {
            log.warn("Order {} is in status {} and cannot be automatically cancelled", 
                    order.getId(), currentStatus);
        }
    }
}
