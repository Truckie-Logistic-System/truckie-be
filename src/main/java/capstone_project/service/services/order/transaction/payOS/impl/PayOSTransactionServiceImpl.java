package capstone_project.service.services.order.transaction.payOS.impl;

import capstone_project.common.enums.*;
import capstone_project.common.exceptions.dto.BadRequestException;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.config.payment.PayOS.PayOSProperties;
import capstone_project.dtos.response.order.transaction.GetTransactionStatusResponse;
import capstone_project.dtos.response.order.transaction.TransactionResponse;
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
import capstone_project.service.mapper.order.TransactionMapper;
import capstone_project.service.services.order.order.OrderService;
import capstone_project.service.services.order.transaction.payOS.PayOSTransactionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import vn.payos.PayOS;
import vn.payos.type.PaymentData;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayOSTransactionServiceImpl implements PayOSTransactionService {

    private final TransactionEntityService transactionEntityService;
    private final ContractEntityService contractEntityService;
    private final OrderEntityService orderEntityService;
    private final CustomerEntityService customerEntityService;
    private final UserEntityService userEntityService;
    private final ContractSettingEntityService contractSettingEntityService;
    private final ObjectProvider<OrderService> orderServiceObjectProvider;

    private final PayOSProperties properties;
    private final PayOS payOS;

    private final TransactionMapper transactionMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public TransactionResponse createTransaction(UUID contractId) {
        log.info("Creating transaction for contract {}", contractId);

        Long payOsOrderCode = System.currentTimeMillis();

        ContractEntity contractEntity = getAndValidateContract(contractId);

        if (!contractEntity.getStatus().equals(ContractStatusEnum.DEPOSITED.name()) &&
                !contractEntity.getStatus().equals(ContractStatusEnum.UNPAID.name()) &&
                !contractEntity.getStatus().equals(ContractStatusEnum.CONTRACT_SIGNED.name())) {
            log.error("Contract {} is not in DEPOSITED status", contractId);
            throw new BadRequestException(
                    "Contract is not in DEPOSITED status",
                    ErrorEnum.INVALID.getErrorCode()
            );
        }

        BigDecimal totalValue = validationTotalValue(contractId);

        int amountForPayOS = totalValue.setScale(0, RoundingMode.HALF_UP).intValueExact();

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

        PaymentData paymentData = PaymentData.builder()
                .orderCode(payOsOrderCode)
                .amount(remainingAmount.setScale(0, RoundingMode.HALF_UP).intValueExact())
                .description("Create transaction")
                .cancelUrl(properties.getCancelUrl())
                .returnUrl(properties.getReturnUrl())
                .build();

        try {
            var response = payOS.createPaymentLink(paymentData);

            TransactionEntity transaction = TransactionEntity.builder()
                    .id(UUID.randomUUID())
                    .amount(remainingAmount)
                    .status(TransactionEnum.PENDING.name())
                    .currencyCode("VND")
                    .paymentProvider("PayOS")
                    .gatewayResponse(objectMapper.writeValueAsString(response))
                    .gatewayOrderCode(String.valueOf(payOsOrderCode))
                    .contractEntity(contractEntity)
                    .build();

            TransactionEntity savedEntity = transactionEntityService.save(transaction);

            return transactionMapper.toTransactionResponse(savedEntity);

        } catch (Exception e) {
            log.error("Error calling PayOS API", e);
            throw new RuntimeException("Failed to create payment link", e);
        }
    }


    @Override
    @Transactional
    public TransactionResponse createDepositTransaction(UUID contractId) {
        log.info("Creating transaction for contract {}", contractId);

        log.info("Creating PayOS PaymentData: cancelUrl={}, returnUrl={}",
                properties.getCancelUrl(), properties.getReturnUrl());

        Long payOsOrderCode = System.currentTimeMillis();

        ContractEntity contractEntity = getAndValidateContract(contractId);

        BigDecimal totalValue = validationTotalValue(contractId);

//        int amountForPayOS = totalValue.setScale(0, RoundingMode.HALF_UP).intValueExact();

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

        PaymentData paymentData = PaymentData.builder()
                .orderCode(payOsOrderCode)
                .amount(depositAmount.setScale(0, RoundingMode.HALF_UP).intValueExact())
//                .amount(4000)
                .description("Create deposit")
//                                .items(List.of(item))
                .cancelUrl(properties.getCancelUrl())
                .returnUrl(properties.getReturnUrl())
                .build();

        try {
            var response = payOS.createPaymentLink(paymentData);

            log.info("PayOS response: {}", objectMapper.writeValueAsString(response));

            TransactionEntity transaction = TransactionEntity.builder()
                    .id(UUID.randomUUID())
                    .amount(depositAmount)
                    .status(TransactionEnum.PENDING.name())
                    .currencyCode("VND")
                    .paymentProvider("PayOS")
                    .gatewayResponse(objectMapper.writeValueAsString(response))
                    .gatewayOrderCode(String.valueOf(payOsOrderCode))
                    .contractEntity(contractEntity)
                    .build();

//            transaction.setStatus(TransactionEnum.DEPOSITED.name());
            TransactionEntity savedEntity = transactionEntityService.save(transaction);
//            contractEntity.setStatus(ContractStatusEnum.DEPOSITED.name());

            return transactionMapper.toTransactionResponse(savedEntity);


        } catch (Exception e) {
            log.error("Error calling PayOS API", e);
            throw new RuntimeException("Failed to create payment link", e);
        }
    }

    private BigDecimal validationTotalValue(UUID contractId) {

        ContractEntity contractEntity = contractEntityService.findEntityById(contractId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        BigDecimal totalValue = contractEntity.getTotalValue();
        log.info("Total value for contract {} is {}", contractId, totalValue);
        BigDecimal supportedValue = contractEntity.getSupportedValue();
        log.info("Supported value for contract {} is {}", contractId, supportedValue);

        if (supportedValue != null && supportedValue.compareTo(BigDecimal.ZERO) > 0) {
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

    @Override
    public TransactionResponse getTransactionById(UUID transactionId) {
        log.info("Fetching transaction with ID: {}", transactionId);

        if (transactionId == null) {
            log.error("Transaction ID is null");
            throw new BadRequestException(
                    "Transaction ID cannot be null",
                    ErrorEnum.INVALID.getErrorCode()
            );
        }

        TransactionEntity transaction = transactionEntityService.findEntityById(transactionId)
                .orElseThrow(() -> new NotFoundException(
                        "Transaction not found",
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        return transactionMapper.toTransactionResponse(transaction);
    }

    @Override
    public List<TransactionResponse> getTransactionsByContractId(UUID contractId) {
        log.info("Fetching transactions for contract {}", contractId);

        if (contractId == null) {
            log.error("Contract ID is null");
            throw new BadRequestException(
                    "Transaction ID cannot be null",
                    ErrorEnum.INVALID.getErrorCode()
            );
        }

        return transactionEntityService.findByContractId(contractId).stream()
                .map(transactionMapper::toTransactionResponse)
                .toList();
    }

    @Override
    public GetTransactionStatusResponse getTransactionStatus(UUID transactionId) {
        log.info("Getting transaction status for ID: {}", transactionId);

        if (transactionId == null) {
            log.error("Transaction ID is null");
            throw new BadRequestException(
                    "Transaction ID cannot be null",
                    ErrorEnum.INVALID.getErrorCode()
            );
        }

        TransactionEntity transaction = transactionEntityService.findEntityById(transactionId)
                .orElseThrow(() -> new NotFoundException(
                        "Transaction not found",
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        return transactionMapper.toGetTransactionStatusResponse(transaction);
    }

    @Override
    @Transactional
    public void handleWebhook(String rawCallbackPayload) {
        log.info("Handling webhook with payload: {}", rawCallbackPayload);
        try {
            JsonNode webhookEvent = objectMapper.readTree(rawCallbackPayload);

            String orderCode = webhookEvent.path("data").path("orderCode").asText(null);
            String payOsStatus = webhookEvent.path("data").path("status").asText(null);
            String payOsCode = webhookEvent.path("data").path("code").asText(null);

            // Skip test webhook
            if ("123".equals(orderCode)) {
                log.info("Received PayOS test webhook with orderCode=123, skipping...");
                return;
            }

            if (orderCode == null) {
                log.error("Invalid webhook payload: {}", rawCallbackPayload);
                return;
            }

            transactionEntityService.findByGatewayOrderCode(orderCode).ifPresentOrElse(transaction -> {
                TransactionEnum mappedStatus = mapPayOsStatusToEnum(payOsStatus, payOsCode);

                transaction.setStatus(mappedStatus.name());
                transaction.setGatewayResponse(rawCallbackPayload);
                transaction.setPaymentDate(java.time.LocalDateTime.now());
                transactionEntityService.save(transaction);

                log.info("Webhook processed successfully. TxnId={}, PayOS status={}, Mapped status={}",
                        transaction.getId(), payOsStatus, mappedStatus);

                updateContractStatusIfNeeded(transaction);
            }, () -> {
                log.warn("Transaction not found for orderCode {}", orderCode);
            });

        } catch (Exception e) {
            log.error("Failed to handle webhook", e);
        }
    }


    private TransactionEnum mapPayOsStatusToEnum(String payOsStatus, String code) {
        if (code != null && code.equals("00")) {
            return TransactionEnum.PAID;
        }

        if (payOsStatus == null) {
            return TransactionEnum.PENDING;
        }

        return switch (payOsStatus.toLowerCase()) {
            case "paid", "success", "completed" -> TransactionEnum.PAID;
            case "cancel", "cancelled", "canceled" -> TransactionEnum.CANCELLED;
            case "failed", "error" -> TransactionEnum.FAILED;
            case "expired", "timeout" -> TransactionEnum.EXPIRED;
            case "refunded" -> TransactionEnum.REFUNDED;
            default -> TransactionEnum.PENDING;
        };
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
                BigDecimal totalPaidAmount = transactionEntityService.sumPaidAmountByContractId(contract.getId());

                log.info(">>>> DEBUG: Total Value = {}, Total Paid Amount from DB = {}", totalValue, totalPaidAmount);

                if (totalPaidAmount == null) {
                    totalPaidAmount = BigDecimal.ZERO;
                }

                if (totalPaidAmount.compareTo(totalValue) >= 0) {
                    log.info("Test1");
                    contract.setStatus(ContractStatusEnum.PAID.name());
                    orderService.changeStatusOrderWithAllOrderDetail(order.getId(), OrderStatusEnum.FULLY_PAID);
                } else {
                    log.info("Test2");
                    contract.setStatus(ContractStatusEnum.DEPOSITED.name());
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

        if (TransactionEnum.valueOf(transaction.getStatus()) == TransactionEnum.REFUNDED) {
            orderEntityService.save(order);
        }

        contractEntityService.save(contract);
        log.info("Contract {} updated to status {}", contract.getId(), contract.getStatus());
    }

//    private void updateContractStatusIfNeeded(TransactionEntity transaction) {
//        ContractEntity contract = transaction.getContractEntity();
//        if (contract == null) {
//            log.warn("Transaction {} has no contract linked", transaction.getId());
//            throw new NotFoundException(
//                    "No contract linked to transaction",
//                    ErrorEnum.NOT_FOUND.getErrorCode()
//            );
//        }
//
//        OrderEntity order = contract.getOrderEntity();
//        if (order == null) {
//            log.warn("No order found for contract {}", contract.getId());
//            throw new NotFoundException(
//                    "No order found for contract",
//                    ErrorEnum.NOT_FOUND.getErrorCode());
//        }
//
//        OrderService orderService = orderServiceObjectProvider.getIfAvailable();
//        if (orderService == null) {
//            log.warn("No order found for contract {}", contract.getId());
//            throw new NotFoundException(
//                    "No order found for contract",
//                    ErrorEnum.NOT_FOUND.getErrorCode());
//        }
//
//        switch (TransactionEnum.valueOf(transaction.getStatus())) {
//            case PAID -> {
//                BigDecimal totalValue = validationTotalValue(contract.getId());
//
//                if (transaction.getAmount().compareTo(totalValue) < 0) {
//                    contract.setStatus(ContractStatusEnum.DEPOSITED.name());
//                    orderService.changeStatusOrderWithAllOrderDetail(order.getId(), OrderStatusEnum.ON_PLANNING);
//                } else {
//                    contract.setStatus(ContractStatusEnum.PAID.name());
//                    orderService.changeStatusOrderWithAllOrderDetail(order.getId(), OrderStatusEnum.FULLY_PAID);
//                }
//            }
//
//            case CANCELLED, EXPIRED, FAILED -> contract.setStatus(ContractStatusEnum.UNPAID.name());
//
//            case REFUNDED -> {
//                contract.setStatus(ContractStatusEnum.REFUNDED.name());
//                order.setStatus(OrderStatusEnum.RETURNED.name());
//            }
//            default -> {
//            }
//        }
//
//        if (TransactionEnum.valueOf(transaction.getStatus()) == TransactionEnum.REFUNDED) {
//            orderEntityService.save(order);
//        }
//
//        contractEntityService.save(contract);
//        log.info("Contract {} updated to status {}", contract.getId(), contract.getStatus());
//    }


    @Override
    public TransactionResponse syncTransaction(UUID transactionId) {
        TransactionEntity transaction = transactionEntityService.findEntityById(transactionId)
                .orElseThrow(() -> new NotFoundException(
                        "Transaction not found", ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        try {
            var payosTransaction = payOS.getPaymentLinkInformation(Long.valueOf(transaction.getGatewayOrderCode()));

            transaction.setStatus(payosTransaction.getStatus());
            transaction.setGatewayResponse(objectMapper.writeValueAsString(payosTransaction));

            TransactionEntity updated = transactionEntityService.save(transaction);
            return transactionMapper.toTransactionResponse(updated);

        } catch (Exception e) {
            log.error("Error syncing transaction {}", transactionId, e);
            throw new RuntimeException("Failed to sync transaction", e);
        }
    }

    @Override
    public TransactionResponse refundTransaction(UUID transactionId, String reason) {
        TransactionEntity transaction = transactionEntityService.findEntityById(transactionId)
                .orElseThrow(() -> new NotFoundException(
                        "Transaction not found", ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        try {
            var refundResponse = payOS.cancelPaymentLink(Long.parseLong(transaction.getGatewayOrderCode()), reason);

            transaction.setStatus(TransactionEnum.REFUNDED.name());
            transaction.setGatewayResponse(objectMapper.writeValueAsString(refundResponse));

            TransactionEntity updated = transactionEntityService.save(transaction);
            return transactionMapper.toTransactionResponse(updated);

        } catch (Exception e) {
            log.error("Error refunding transaction {}", transactionId, e);
            throw new RuntimeException("Failed to refund transaction", e);
        }
    }
}
