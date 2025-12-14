package capstone_project.service.services.order.transaction.stripe.impl;

import capstone_project.common.enums.*;
import capstone_project.common.exceptions.dto.BadRequestException;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.config.payment.Stripe.StripeConfig;
import capstone_project.entity.auth.UserEntity;
import capstone_project.entity.order.contract.ContractEntity;
import capstone_project.entity.order.order.OrderEntity;
import capstone_project.entity.order.transaction.TransactionEntity;
import capstone_project.entity.setting.ContractSettingEntity;
import capstone_project.entity.user.customer.CustomerEntity;
import capstone_project.dtos.response.order.transaction.GetTransactionStatusResponse;
import capstone_project.dtos.response.order.transaction.TransactionResponse;
import capstone_project.dtos.response.order.CreateOrderResponse;
import capstone_project.repository.entityServices.auth.UserEntityService;
import capstone_project.repository.entityServices.order.contract.ContractEntityService;
import capstone_project.repository.entityServices.order.order.OrderEntityService;
import capstone_project.repository.entityServices.order.transaction.TransactionEntityService;
import capstone_project.repository.entityServices.setting.ContractSettingEntityService;
import capstone_project.repository.entityServices.user.CustomerEntityService;
import capstone_project.service.services.order.order.OrderService;
import capstone_project.service.services.order.transaction.stripe.StripeTransactionService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import capstone_project.common.utils.VietnamTimeUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripeTransactionServiceImpl implements StripeTransactionService {

    private final ContractEntityService contractEntityService;
    private final TransactionEntityService transactionEntityService;
    private final CustomerEntityService customerEntityService;
    private final OrderEntityService orderEntityService;
    private final UserEntityService userEntityService;
    private final ContractSettingEntityService contractSettingEntityService;
    private final StripeConfig stripeConfig;
    private final ObjectProvider<OrderService> orderServiceObjectProvider;
    private final capstone_project.service.services.order.order.OrderStatusWebSocketService orderStatusWebSocketService;

    @Override
    @Transactional
    public PaymentIntent createPaymentIntent(UUID contractId) throws StripeException {
        
        ContractEntity contractEntity = getAndValidateContract(contractId);

        BigDecimal totalValue = validationTotalValue(contractId);

        if (!contractEntity.getStatus().equals(ContractStatusEnum.DEPOSITED.name()) &&
                !contractEntity.getStatus().equals(ContractStatusEnum.UNPAID.name()) &&
                !contractEntity.getStatus().equals(ContractStatusEnum.CONTRACT_SIGNED.name())) {
            log.error("Contract {} is not in DEPOSITED status", contractId);
            throw new BadRequestException(
                    "Contract is not in DEPOSITED status",
                    ErrorEnum.INVALID.getErrorCode()
            );
        }

        // Get deposit percent: prioritize contract's custom value, fallback to global setting
        BigDecimal depositPercent = getEffectiveDepositPercent(contractEntity);
        log.info("📊 Stripe remaining payment - Using deposit percent: {}% (custom: {})", depositPercent, 
            contractEntity.getCustomDepositPercent() != null ? "yes" : "no");

        BigDecimal depositAmount = totalValue.multiply(depositPercent)
                .divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);

        BigDecimal remainingAmount = totalValue.subtract(depositAmount);

        long remainingAmountInVND = remainingAmount.longValueExact();

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(remainingAmountInVND)
                .setCurrency("vnd")
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .setAllowRedirects(PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
                                .build()
                )
                .build();

        RequestOptions options = RequestOptions.builder()
                .setIdempotencyKey(UUID.randomUUID().toString())
                .build();

        try {
            PaymentIntent paymentIntent = PaymentIntent.create(params, options);

            TransactionEntity transaction = TransactionEntity.builder()
                    .id(UUID.randomUUID())
                    .amount(remainingAmount)
                    .status(TransactionEnum.PENDING.name())
                    .currencyCode("VND")
                    .paymentProvider("Stripe")
                    .gatewayResponse(paymentIntent.getClientSecret())
                    .gatewayOrderCode(paymentIntent.getId())
                    .contractEntity(contractEntity)
                    .build();

            transactionEntityService.save(transaction);

            return paymentIntent;
        } catch (StripeException ex) {
            log.error("Stripe API error while creating payment intent: {}", ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error while creating Stripe payment intent", ex);
            throw new RuntimeException("Failed to create Stripe payment intent", ex);
        }
    }

    @Override
    @Transactional
    public PaymentIntent createDepositPaymentIntent(UUID contractId) throws StripeException {

        ContractEntity contractEntity = getAndValidateContract(contractId);

        BigDecimal totalValue = validationTotalValue(contractId);

        if (!contractEntity.getStatus().equals(ContractStatusEnum.CONTRACT_SIGNED.name()) && !contractEntity.getStatus().equals(ContractStatusEnum.UNPAID.name())) {
            log.error("Contract {} is not in CONTRACT_SIGNED or UNPAID status", contractId);
            throw new BadRequestException(
                    "Contract is not in CONTRACT_SIGNED or UNPAID status",
                    ErrorEnum.INVALID.getErrorCode()
            );
        }

        // Get deposit percent: prioritize contract's custom value, fallback to global setting
        BigDecimal depositPercent = getEffectiveDepositPercent(contractEntity);
        log.info("📊 Stripe deposit payment - Using deposit percent: {}% (custom: {})", depositPercent, 
            contractEntity.getCustomDepositPercent() != null ? "yes" : "no");

        BigDecimal depositAmount = totalValue.multiply(depositPercent)
                .divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);

        long depositAmountInVND = depositAmount.longValueExact();

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(depositAmountInVND)
                .setCurrency("vnd")
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
//                                .setAllowRedirects(PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
                                .build()
                )
                .build();

        RequestOptions options = RequestOptions.builder()
                .setIdempotencyKey(UUID.randomUUID().toString())
                .build();

        try {
            PaymentIntent paymentIntent = PaymentIntent.create(params, options);

            TransactionEntity transaction = TransactionEntity.builder()
                    .id(UUID.randomUUID())
                    .amount(depositAmount)
                    .status(TransactionEnum.PENDING.name())
                    .currencyCode("VND")
                    .paymentProvider("Stripe")
                    .gatewayResponse(paymentIntent.getClientSecret())
                    .gatewayOrderCode(paymentIntent.getId())
                    .contractEntity(contractEntity)
                    .build();

            transactionEntityService.save(transaction);

            return paymentIntent;
        } catch (StripeException ex) {
            log.error("Stripe API error while creating payment intent: {}", ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error while creating Stripe payment intent", ex);
            throw new RuntimeException("Failed to create Stripe payment intent", ex);
        }
    }

    @Override
    public void handleStripeWebhook(String payload, String sigHeader) {
        
        Event event;

        try {
            event = Webhook.constructEvent(payload, sigHeader, stripeConfig.getSecretWebhookKey());
        } catch (SignatureVerificationException e) {
            log.error("Webhook signature verification failed.", e);
            throw new RuntimeException("Invalid Stripe signature", e);
        }

        try {
            switch (event.getType()) {
                case "payment_intent.succeeded" -> {
                    PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer()
                            .getObject()
                            .orElseThrow(() -> new RuntimeException("Failed to deserialize PaymentIntent"));

                    if (!"succeeded".equals(paymentIntent.getStatus())) {
                        log.warn("Received payment_intent.succeeded but status is {}", paymentIntent.getStatus());
                        return;
                    }

                    transactionEntityService.findByGatewayOrderCode(paymentIntent.getId())
                            .ifPresentOrElse(transaction -> {
                                transaction.setStatus(TransactionEnum.PAID.name());
                                transaction.setGatewayResponse(payload);
                                transaction.setPaymentDate(VietnamTimeUtils.now());
                                transactionEntityService.save(transaction);
                                updateContractStatusIfNeeded(transaction);
                                
                            }, () -> {
                                log.warn("Transaction not found for PaymentIntent {}", paymentIntent.getId());
                            });
                }

                case "payment_intent.payment_failed" -> {
                    PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer()
                            .getObject()
                            .orElseThrow(() -> new RuntimeException("Failed to deserialize PaymentIntent"));

                    log.warn("PaymentIntent failed: {}", paymentIntent.getId());

                    transactionEntityService.findByGatewayOrderCode(paymentIntent.getId())
                            .ifPresentOrElse(transaction -> {
                                transaction.setStatus(TransactionEnum.FAILED.name());
                                transaction.setGatewayResponse(payload);
                                transactionEntityService.save(transaction);
                            }, () -> log.warn("Transaction not found for failed PaymentIntent {}", paymentIntent.getId()));
                }

                default -> {
                    // Unhandled event type
                }
            }
        } catch (Exception e) {
            log.error("Error processing Stripe webhook", e);
            throw new RuntimeException("Error processing Stripe webhook", e);
        }
    }

    private void updateContractStatusIfNeeded(TransactionEntity transaction) {
        ContractEntity contract = transaction.getContractEntity();
        if (contract == null) {
            log.warn("Transaction {} has no contract linked", transaction.getId());
            throw new NotFoundException(
                    "No contract linked to transaction",
                    ErrorEnum.NOT_FOUND.getErrorCode()
            );
        }

        OrderEntity order = contract.getOrderEntity();
        if (order == null) {
            log.warn("No order found for contract {}", contract.getId());
            throw new NotFoundException(
                    "No order found for contract",
                    ErrorEnum.NOT_FOUND.getErrorCode());
        }

        OrderService orderService = orderServiceObjectProvider.getIfAvailable();
        if (orderService == null) {
            log.warn("No order found for contract {}", contract.getId());
            throw new NotFoundException(
                    "No order found for contract",
                    ErrorEnum.NOT_FOUND.getErrorCode());
        }

        switch (TransactionEnum.valueOf(transaction.getStatus())) {
            case PAID -> {
                BigDecimal totalValue = validationTotalValue(contract.getId());

                if (transaction.getAmount().compareTo(totalValue) < 0) {
                    contract.setStatus(ContractStatusEnum.DEPOSITED.name());
                    // Update both order and all order details status to ON_PLANNING
                    CreateOrderResponse response = orderService.changeStatusOrderWithAllOrderDetail(order.getId(), OrderStatusEnum.ON_PLANNING);
                    
                    log.info("✅ Order {} and all order details status updated to ON_PLANNING", 
                        order.getOrderCode());
                } else {
                    contract.setStatus(ContractStatusEnum.PAID.name());
                    // Update Order status only (OrderDetail will be updated when assigned to driver)
                    OrderStatusEnum previousStatus = OrderStatusEnum.valueOf(order.getStatus());
                    order.setStatus(OrderStatusEnum.FULLY_PAID.name());
                    orderEntityService.save(order);
                    
                    // Send WebSocket notification
                    orderStatusWebSocketService.sendOrderStatusChange(
                            order.getId(),
                            order.getOrderCode(),
                            previousStatus,
                            OrderStatusEnum.FULLY_PAID
                    );
                }
            }

            case CANCELLED, EXPIRED, FAILED -> contract.setStatus(ContractStatusEnum.UNPAID.name());

            case REFUNDED -> {
                contract.setStatus(ContractStatusEnum.REFUNDED.name());

                // ✅ IMPORTANT: Do NOT set Order status to RETURNED directly here.
                // RETURNING/RETURNED are max statuses and must be enforced via aggregation
                // across all OrderDetails. Delegate to OrderService.updateOrderStatus which
                // already validates that ALL order details are in the proper return status.
                try {
                    orderService.updateOrderStatus(order.getId(), OrderStatusEnum.RETURNED);
                    log.info("✅ Aggregated Order status update to RETURNED after Stripe REFUNDED for order {}", order.getId());
                } catch (Exception e) {
                    log.error("❌ Failed to update Order status to RETURNED via aggregation for order {}: {}",
                            order.getId(), e.getMessage());
                    // Don't throw - aggregation enforcement failure shouldn't break refund processing
                }
            }
            default -> {
            }
        }

        if (TransactionEnum.valueOf(transaction.getStatus()) == TransactionEnum.REFUNDED) {
            // Order status (including RETURNED) is now managed via aggregation logic in OrderService
            // and/or OrderDetailStatusService, so we don't need to force-save the Order here
            // unless it was actually modified above.
            orderEntityService.save(order);
        }

        contractEntityService.save(contract);
        
    }

    private BigDecimal validationTotalValue(UUID contractId) {

        ContractEntity contractEntity = contractEntityService.findEntityById(contractId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        BigDecimal totalValue = contractEntity.getTotalValue();
        
        BigDecimal adjustedValue = contractEntity.getAdjustedValue();

        if (adjustedValue != null && adjustedValue.compareTo(BigDecimal.ZERO) > 0) {
            totalValue = adjustedValue;
        }

        if (totalValue == null || totalValue.compareTo(BigDecimal.ZERO) <= 0) {
            log.error("Contract {} has invalid total value: {}", contractId, totalValue);
            throw new BadRequestException(
                    "Invalid contract total value",
                    ErrorEnum.INVALID.getErrorCode()
            );
        }
        return totalValue;
    }

    private ContractEntity getAndValidateContract(UUID contractId) {
        ContractEntity contractEntity = contractEntityService.findEntityById(contractId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        OrderEntity orderEntity = orderEntityService.findEntityById(contractEntity.getOrderEntity().getId())
                .orElseThrow(() -> new NotFoundException(
                        "Order not found for contract",
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        CustomerEntity customerEntity = customerEntityService.findEntityById(orderEntity.getSender().getId())
                .orElseThrow(() -> new NotFoundException(
                        "Customer not found for order",
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        if (!customerEntity.getStatus().equals(CommonStatusEnum.ACTIVE.name())) {
            log.error("Customer {} is not active", customerEntity.getId());
            throw new BadRequestException(
                    "Customer is not active",
                    ErrorEnum.INVALID.getErrorCode()
            );
        }

        UserEntity userEntity = userEntityService.findEntityById(customerEntity.getUser().getId())
                .orElseThrow(() -> new NotFoundException(
                        "User not found for customer",
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        if (!userEntity.getStatus().equals(CommonStatusEnum.ACTIVE.name())) {
            log.error("User {} is not active", userEntity.getId());
            throw new BadRequestException(
                    "User is not active",
                    ErrorEnum.INVALID.getErrorCode()
            );
        }

        if (ContractStatusEnum.PAID.name().equals(contractEntity.getStatus())) {
            log.error("Contract {} is already fully paid!", contractId);
            throw new BadRequestException(
                    "Contract is already fully paid!",
                    ErrorEnum.INVALID.getErrorCode()
            );
        }

//        if (transactionEntityService.existsByContractIdAndStatus(contractId, TransactionEnum.PAID.name())
//                && contractEntity.getStatus().equals(ContractStatusEnum.DEPOSITED.name())) {
//            log.error("Contract {} is already fully paid!", contractId);
//            throw new BadRequestException(
//                    "Contract is already fully paid!",
//                    ErrorEnum.INVALID.getErrorCode()
//            );
//        }

        return contractEntity;
    }
    
    /**
     * Get effective deposit percent for a contract.
     * Prioritizes contract's custom deposit percent if set, otherwise falls back to global setting.
     * 
     * @param contractEntity The contract to get deposit percent for
     * @return The effective deposit percent (0-100)
     */
    private BigDecimal getEffectiveDepositPercent(ContractEntity contractEntity) {
        // First, check if contract has custom deposit percent
        if (contractEntity.getCustomDepositPercent() != null 
            && contractEntity.getCustomDepositPercent().compareTo(BigDecimal.ZERO) > 0
            && contractEntity.getCustomDepositPercent().compareTo(BigDecimal.valueOf(100)) <= 0) {
            return contractEntity.getCustomDepositPercent();
        }
        
        // Fallback to global setting
        ContractSettingEntity setting = contractSettingEntityService.findFirstByOrderByCreatedAtAsc()
                .orElseThrow(() -> new NotFoundException(
                        "Contract settings not found",
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));
        
        BigDecimal depositPercent = setting.getDepositPercent() != null ? setting.getDepositPercent() : BigDecimal.ZERO;
        if (depositPercent.compareTo(BigDecimal.ZERO) <= 0 || depositPercent.compareTo(BigDecimal.valueOf(100)) > 0) {
            log.error("Invalid deposit percent in contract settings: {}", depositPercent);
            throw new BadRequestException(
                    "Invalid deposit percent in contract settings",
                    ErrorEnum.INVALID.getErrorCode()
            );
        }
        
        return depositPercent;
    }
}
