package capstone_project.service.services.order.transaction.impl;

import capstone_project.common.enums.*;
import capstone_project.common.exceptions.dto.BadRequestException;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.config.payment.PayOSProperties;
import capstone_project.dtos.response.order.transaction.GetTransactionStatusResponse;
import capstone_project.dtos.response.order.transaction.TransactionResponse;
import capstone_project.entity.auth.UserEntity;
import capstone_project.entity.order.contract.ContractEntity;
import capstone_project.entity.order.order.OrderEntity;
import capstone_project.entity.order.transaction.TransactionEntity;
import capstone_project.entity.user.customer.CustomerEntity;
import capstone_project.repository.entityServices.auth.UserEntityService;
import capstone_project.repository.entityServices.order.contract.ContractEntityService;
import capstone_project.repository.entityServices.order.order.OrderEntityService;
import capstone_project.repository.entityServices.order.transaction.TransactionEntityService;
import capstone_project.repository.entityServices.user.CustomerEntityService;
import capstone_project.service.mapper.order.TransactionMapper;
import capstone_project.service.services.order.transaction.TransactionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class TransactionServiceImpl implements TransactionService {

    private final TransactionEntityService transactionEntityService;
    private final ContractEntityService contractEntityService;
    private final OrderEntityService orderEntityService;
    private final CustomerEntityService customerEntityService;
    private final UserEntityService userEntityService;
    private final PayOSProperties properties;
    private final PayOS payOS;
    private final TransactionMapper transactionMapper;
    private final ObjectMapper objectMapper;

    @Override
    public TransactionResponse createTransaction(UUID contractId) {
        log.info("Creating transaction for contract {}", contractId);

        Long payOsOrderCode = System.currentTimeMillis();

        ContractEntity contractEntity = getAndValidateContract(contractId);

        BigDecimal totalValue = validationTotalValue(contractId, contractEntity);

        int amountForPayOS = totalValue.setScale(0, RoundingMode.HALF_UP).intValueExact();

        PaymentData paymentData = PaymentData.builder()
                .orderCode(payOsOrderCode)
                .amount(amountForPayOS)
                .description("Create transaction")
//                                .items(List.of(item))
                .cancelUrl(properties.getCancelUrl())
                .returnUrl(properties.getReturnUrl())
                .build();

        try {
            var response = payOS.createPaymentLink(paymentData);

            TransactionEntity transaction = TransactionEntity.builder()
                    .id(UUID.randomUUID())
                    .amount(BigDecimal.valueOf(amountForPayOS))
                    .status(TransactionEnum.PENDING.name())
                    .currencyCode("VND")
                    .paymentProvider("PayOS")
                    .gatewayResponse(objectMapper.writeValueAsString(response))
                    .gatewayOrderCode(payOsOrderCode)
                    .contractEntity(contractEntity)
                    .build();

            TransactionEntity savedEntity = transactionEntityService.save(transaction);

            return transactionMapper.toTransactionResponse(savedEntity);


        } catch (Exception e) {
            log.error("Error calling PayOS API", e);
            throw new RuntimeException("Failed to create payment link", e);
        }
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

        OrderEntity orderEntity = orderEntityService.findEntityById(contractEntity.getId())
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

        if (!ContractStatusEnum.UNPAID.name().equals(contractEntity.getStatus())
                && !ContractStatusEnum.CONTRACT_SIGNED.name().equals(contractEntity.getStatus())) {
            log.error("Contract {} is not in UNPAID or CONTRACT_SIGNED status", contractId);
            throw new BadRequestException(
                    "Contract is not eligible for payment",
                    ErrorEnum.INVALID.getErrorCode()
            );
        }

        if (transactionEntityService.existsByContractIdAndStatus(contractId, TransactionEnum.PAID.name())) {
            log.error("Contract {} is already paid successfully !", contractId);
            throw new BadRequestException(
                    "Contract is already paid successfully !",
                    ErrorEnum.INVALID.getErrorCode()
            );
        }

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
    public void handleWebhook(String rawCallbackPayload) {
        log.info("Handling webhook with payload: {}", rawCallbackPayload);
        try {
            JsonNode webhookEvent = objectMapper.readTree(rawCallbackPayload);

            Long orderCode = webhookEvent.path("data").path("orderCode").asLong();
            String payOsStatus = webhookEvent.path("data").path("status").asText();

            if (orderCode == null || payOsStatus == null) {
                log.error("Invalid webhook payload: {}", rawCallbackPayload);
                throw new BadRequestException("Invalid webhook payload", ErrorEnum.INVALID.getErrorCode());
            }

            TransactionEntity transaction = transactionEntityService.findByGatewayOrderCode(orderCode)
                    .orElseThrow(() -> new NotFoundException(
                            "Transaction not found for orderCode " + orderCode,
                            ErrorEnum.NOT_FOUND.getErrorCode()
                    ));

//            String mappedStatus = mapPayOsStatusToTransactionStatus(payOsStatus);

            transaction.setStatus(payOsStatus.toUpperCase());
            transaction.setGatewayResponse(rawCallbackPayload);

            transactionEntityService.save(transaction);

            log.info("Webhook processed successfully. TxnId={}, PayOS status={}, Mapped status={}",
                    transaction.getId(), payOsStatus, payOsStatus.toUpperCase());

            updateContractStatusIfNeeded(transaction);

        } catch (Exception e) {
            log.error("Failed to handle webhook", e);
            throw new RuntimeException("Webhook processing error", e);
        }
    }

//    private String mapPayOsStatusToTransactionStatus(String payOsStatus) {
//        return switch (payOsStatus.toUpperCase()) {
//            case "PAID" -> TransactionEnum.PAID.name();
//            case "CANCELLED" -> TransactionEnum.CANCELLED.name();
//            case "EXPIRED" -> TransactionEnum.EXPIRED.name();
//            case "FAILED" -> TransactionEnum.FAILED.name();
//            case "REFUNDED" -> TransactionEnum.REFUNDED.name();
//            case "PENDING" -> TransactionEnum.PENDING.name();
//            default -> TransactionEnum.FAILED.name();
//        };
//    }

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
                contract.setStatus(ContractStatusEnum.PAID.name());
                order.setStatus(OrderStatusEnum.SUCCESSFUL.name());
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


    @Override
    public TransactionResponse syncTransaction(UUID transactionId) {
        TransactionEntity transaction = transactionEntityService.findEntityById(transactionId)
                .orElseThrow(() -> new NotFoundException(
                        "Transaction not found", ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        try {
            var payosTransaction = payOS.getPaymentLinkInformation(transaction.getGatewayOrderCode());

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
            var refundResponse = payOS.cancelPaymentLink(transaction.getGatewayOrderCode(), reason);

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
