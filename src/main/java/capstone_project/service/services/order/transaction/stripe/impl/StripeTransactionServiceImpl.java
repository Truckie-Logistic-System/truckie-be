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

    @Override
    @Transactional
    public PaymentIntent createPaymentIntent(UUID contractId) throws StripeException {
        log.info("createPaymentIntent - Stripe");
        ContractEntity contractEntity = getAndValidateContract(contractId);

        BigDecimal totalValue = validationTotalValue(contractId, contractEntity);

        if (!contractEntity.getStatus().equals(ContractStatusEnum.DEPOSITED.name()) &&
                !contractEntity.getStatus().equals(ContractStatusEnum.UNPAID.name()) &&
                !contractEntity.getStatus().equals(ContractStatusEnum.CONTRACT_SIGNED.name())) {
            log.error("Contract {} is not in DEPOSITED status", contractId);
            throw new BadRequestException(
                    "Contract is not in DEPOSITED status",
                    ErrorEnum.INVALID.getErrorCode()
            );
        }

        ContractSettingEntity setting = contractSettingEntityService.findFirstByOrderByCreatedAtAsc()
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage(),
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

            log.info("Created Stripe PaymentIntent: {} for contract {}", paymentIntent.getId(), contractId);

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
        log.info("createDepositPaymentIntent - Stripe");

        ContractEntity contractEntity = getAndValidateContract(contractId);

        BigDecimal totalValue = validationTotalValue(contractId, contractEntity);

        if (!contractEntity.getStatus().equals(ContractStatusEnum.CONTRACT_SIGNED.name()) && !contractEntity.getStatus().equals(ContractStatusEnum.UNPAID.name())) {
            log.error("Contract {} is not in CONTRACT_SIGNED or UNPAID status", contractId);
            throw new BadRequestException(
                    "Contract is not in CONTRACT_SIGNED or UNPAID status",
                    ErrorEnum.INVALID.getErrorCode()
            );
        }

        ContractSettingEntity setting = contractSettingEntityService.findFirstByOrderByCreatedAtAsc()
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage(),
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

            log.info("Created Stripe PaymentIntent: {} for contract {}", paymentIntent.getId(), contractId);

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
        log.info("Received Stripe webhook: {}", payload);
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

                    log.info("PaymentIntent succeeded: {}", paymentIntent.getId());

                    if (!"succeeded".equals(paymentIntent.getStatus())) {
                        log.warn("Received payment_intent.succeeded but status is {}", paymentIntent.getStatus());
                        return;
                    }

                    transactionEntityService.findByGatewayOrderCode(paymentIntent.getId())
                            .ifPresentOrElse(transaction -> {
                                transaction.setStatus(TransactionEnum.PAID.name());
                                transaction.setGatewayResponse(payload);
                                transactionEntityService.save(transaction);
                                updateContractStatusIfNeeded(transaction);
                                log.info("Transaction {} marked as PAID", transaction.getId());
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


                default -> log.info("Unhandled event type: {}", event.getType());
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
            log.warn("Contract {} has no order linked", contract.getId());
            throw new NotFoundException(
                    "No contract linked to transaction",
                    ErrorEnum.NOT_FOUND.getErrorCode()
            );
        }

        switch (TransactionEnum.valueOf(transaction.getStatus())) {
            case PAID -> {
                BigDecimal totalValue = validationTotalValue(contract.getId(), contract);

                if (transaction.getAmount().compareTo(totalValue) < 0) {
                    contract.setStatus(ContractStatusEnum.DEPOSITED.name());
                } else {
                    OrderService orderService = orderServiceObjectProvider.getIfAvailable();
                    if (orderService == null) {
                        throw new RuntimeException("OrderService is not available");
                    }
                    contract.setStatus(ContractStatusEnum.PAID.name());
                    orderService.changeStatusOrderWithAllOrderDetail(order.getId(), OrderStatusEnum.ON_PLANNING);
                }
            }
            case CANCELLED, EXPIRED, FAILED -> contract.setStatus(ContractStatusEnum.UNPAID.name());
            case REFUNDED -> {
                contract.setStatus(ContractStatusEnum.REFUNDED.name());
                order.setStatus(OrderStatusEnum.RETURNED.name());
            }
            default -> {
            }
        }

        orderEntityService.save(order);
        contractEntityService.save(contract);
        log.info("Contract {} updated to status {}", contract.getId(), contract.getStatus());
    }

    private static BigDecimal validationTotalValue(UUID contractId, ContractEntity contractEntity) {
        BigDecimal totalValue = contractEntity.getTotalValue();
        BigDecimal supportedValue = contractEntity.getSupportedValue();

        if (supportedValue != null) {
            totalValue = supportedValue;
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
}
