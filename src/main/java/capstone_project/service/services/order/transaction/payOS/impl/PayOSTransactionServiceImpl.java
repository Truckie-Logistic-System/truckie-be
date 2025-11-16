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
import vn.payos.exception.PayOSException;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
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
    
    // ORDER_REJECTION dependencies
    private final capstone_project.repository.entityServices.issue.IssueEntityService issueEntityService;
    private final capstone_project.repository.entityServices.order.order.JourneyHistoryEntityService journeyHistoryEntityService;
    private final capstone_project.repository.entityServices.order.order.OrderDetailEntityService orderDetailEntityService;
    private final capstone_project.service.services.websocket.IssueWebSocketService issueWebSocketService;
    private final capstone_project.service.services.order.order.OrderStatusWebSocketService orderStatusWebSocketService;

    private final PayOSProperties properties;
    private final PayOS payOS;
    private final CustomPayOSClient customPayOSClient;

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
        int finalAmount = remainingAmount.setScale(0, RoundingMode.HALF_UP).intValueExact();
        
        // Append orderId to returnUrl and cancelUrl for proper navigation after payment
        UUID orderId = contractEntity.getOrderEntity() != null ? contractEntity.getOrderEntity().getId() : null;
        String returnUrl = properties.getReturnUrl() + (orderId != null ? "?orderId=" + orderId : "");
        String cancelUrl = properties.getCancelUrl() + (orderId != null ? "?orderId=" + orderId : "");
        
        log.info("🔄 Trying PayOS SDK 2.0.1 first...");
        
        JsonNode response = null;
        boolean sdkSuccess = false;

        try {
            // Try PayOS SDK 2.0.1 first
            CreatePaymentLinkRequest paymentData = CreatePaymentLinkRequest.builder()
                .orderCode(payOsOrderCode)
                .amount((long) finalAmount)
                .description("Create transaction")
                .returnUrl(returnUrl)
                .cancelUrl(cancelUrl)
                .build();
            
            log.info("📤 Calling PayOS SDK 2.0.1 paymentRequests().create()...");
            CreatePaymentLinkResponse sdkResponse = payOS.paymentRequests().create(paymentData);
            
            // Convert SDK response to JSON
            String responseJson = String.format("""
                {
                  "checkoutUrl": "%s",
                  "paymentLinkId": "%s",
                  "orderCode": %d,
                  "amount": %d,
                  "status": "%s"
                }
                """,
                sdkResponse.getCheckoutUrl(),
                sdkResponse.getPaymentLinkId(),
                sdkResponse.getOrderCode(),
                sdkResponse.getAmount(),
                sdkResponse.getStatus()
            );
            
            response = objectMapper.readValue(responseJson, JsonNode.class);
            sdkSuccess = true;
            log.info("✅ PayOS SDK 2.0.1 SUCCESS!");
            
        } catch (Exception sdkEx) {
            log.warn("⚠️ PayOS SDK 2.0.1 FAILED: {}", sdkEx.getMessage());
            log.info("🔄 Falling back to CustomPayOSClient...");
            
            try {
                // Fallback to CustomPayOSClient
                var customResponse = customPayOSClient.createPaymentLink(
                    payOsOrderCode,
                    finalAmount,
                    "Create transaction",
                    returnUrl,
                    cancelUrl
                );
                
                // Convert to JSON
                String responseJson = String.format("""
                    {
                      "checkoutUrl": "%s",
                      "paymentLinkId": "%s",
                      "orderCode": %d,
                      "amount": %d,
                      "status": "%s"
                    }
                    """,
                    customResponse.checkoutUrl(),
                    customResponse.paymentLinkId(),
                    customResponse.orderCode(),
                    customResponse.amount(),
                    customResponse.status()
                );
                
                response = objectMapper.readValue(responseJson, JsonNode.class);
                log.info("✅ CustomPayOSClient SUCCESS (Fallback)");
                
            } catch (Exception customEx) {
                log.error("❌ Both SDK and CustomPayOSClient FAILED!");
                throw new RuntimeException("Failed to create payment link", customEx);
            }
        }
        
        try {
            log.info("✅ Payment link created successfully! (Method: {})", sdkSuccess ? "SDK 2.0.1" : "CustomPayOSClient");

            TransactionEntity transaction = TransactionEntity.builder()
                    .id(UUID.randomUUID())
                    .amount(remainingAmount)
                    .status(TransactionEnum.PENDING.name())
                    .currencyCode("VND")
                    .paymentProvider("PayOS")
                    .transactionType(capstone_project.common.enums.TransactionTypeEnum.FULL_PAYMENT.name())
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

        int finalAmount = depositAmount.setScale(0, RoundingMode.HALF_UP).intValueExact();
        
        // Append orderId to returnUrl and cancelUrl for proper navigation after payment
        UUID orderId = contractEntity.getOrderEntity() != null ? contractEntity.getOrderEntity().getId() : null;
        String returnUrl = properties.getReturnUrl() + (orderId != null ? "?orderId=" + orderId : "");
        String cancelUrl = properties.getCancelUrl() + (orderId != null ? "?orderId=" + orderId : "");
        
        log.info("========== PAYOS DEBUG START ==========");
        log.info("🔑 PayOS Credentials Check:");
        log.info("   Client ID: {}", properties.getClientId());
        log.info("   API Key: {}", properties.getApiKey() != null ? "***" + properties.getApiKey().substring(Math.max(0, properties.getApiKey().length() - 4)) : "null");
        log.info("   Checksum Key: {}", properties.getChecksumKey() != null ? "***" + properties.getChecksumKey().substring(Math.max(0, properties.getChecksumKey().length() - 4)) : "null");
        log.info("   Base URL: {}", properties.getBaseUrl());
        
        log.info("📦 PaymentData Fields:");
        log.info("   orderCode: {}", payOsOrderCode);
        log.info("   amount: {} (from depositAmount: {})", finalAmount, depositAmount);
        log.info("   description: 'Create deposit' (length: {})", "Create deposit".length());
        log.info("   cancelUrl: {}", cancelUrl);
        log.info("   returnUrl: {}", returnUrl);
        
        log.info("🔄 Trying PayOS SDK 2.0.1 first...");
        
        JsonNode response = null;
        boolean sdkSuccess = false;
        
        try {
            // Try PayOS SDK 2.0.1 first
            CreatePaymentLinkRequest paymentData = CreatePaymentLinkRequest.builder()
                .orderCode(payOsOrderCode)
                .amount((long) finalAmount)
                .description("Create deposit")
                .returnUrl(returnUrl)
                .cancelUrl(cancelUrl)
                .build();
            
            log.info("📤 Calling PayOS SDK 2.0.1 paymentRequests().create()...");
            CreatePaymentLinkResponse sdkResponse = payOS.paymentRequests().create(paymentData);
            
            // Convert SDK response to JSON
            String responseJson = String.format("""
                {
                  "checkoutUrl": "%s",
                  "paymentLinkId": "%s",
                  "orderCode": %d,
                  "amount": %d,
                  "status": "%s"
                }
                """,
                sdkResponse.getCheckoutUrl(),
                sdkResponse.getPaymentLinkId(),
                sdkResponse.getOrderCode(),
                sdkResponse.getAmount(),
                sdkResponse.getStatus()
            );
            
            response = objectMapper.readValue(responseJson, JsonNode.class);
            sdkSuccess = true;
            log.info("✅ PayOS SDK 2.0.1 SUCCESS!");
            
        } catch (Exception sdkEx) {
            log.warn("⚠️ PayOS SDK 2.0.1 FAILED: {}", sdkEx.getMessage());
            log.info("🔄 Falling back to CustomPayOSClient...");
            
            try {
                // Fallback to CustomPayOSClient
                var customResponse = customPayOSClient.createPaymentLink(
                    payOsOrderCode,
                    finalAmount,
                    "Create deposit",
                    returnUrl,
                    cancelUrl
                );
                
                // Convert to JSON
                String responseJson = String.format("""
                    {
                      "checkoutUrl": "%s",
                      "paymentLinkId": "%s",
                      "orderCode": %d,
                      "amount": %d,
                      "status": "%s"
                    }
                    """,
                    customResponse.checkoutUrl(),
                    customResponse.paymentLinkId(),
                    customResponse.orderCode(),
                    customResponse.amount(),
                    customResponse.status()
                );
                
                response = objectMapper.readValue(responseJson, JsonNode.class);
                log.info("✅ CustomPayOSClient SUCCESS (Fallback)");
                
            } catch (Exception customEx) {
                log.error("❌ Both SDK and CustomPayOSClient FAILED!");
                throw new RuntimeException("Failed to create payment link", customEx);
            }
        }
        
        try {
            
            log.info("✅ Payment link created successfully! (Method: {})", sdkSuccess ? "SDK 2.0.1" : "CustomPayOSClient");
            log.info("📥 PayOS response: {}", objectMapper.writeValueAsString(response));
            log.info("========== PAYOS DEBUG END ==========");

            TransactionEntity transaction = TransactionEntity.builder()
                    .id(UUID.randomUUID())
                    .amount(depositAmount)
                    .status(TransactionEnum.PENDING.name())
                    .currencyCode("VND")
                    .paymentProvider("PayOS")
                    .transactionType(capstone_project.common.enums.TransactionTypeEnum.DEPOSIT.name())
                    .gatewayResponse(objectMapper.writeValueAsString(response))
                    .gatewayOrderCode(String.valueOf(payOsOrderCode))
                    .contractEntity(contractEntity)
                    .build();

//            transaction.setStatus(TransactionEnum.DEPOSITED.name());
            TransactionEntity savedEntity = transactionEntityService.save(transaction);
//            contractEntity.setStatus(ContractStatusEnum.DEPOSITED.name());

            return transactionMapper.toTransactionResponse(savedEntity);


        } catch (Exception e) {
            log.error("========== PAYOS ERROR (DEPOSIT) ==========");
            log.error("❌ Exception Type: {}", e.getClass().getName());
            log.error("❌ Error Message: {}", e.getMessage());
            log.error("❌ Cause: {}", e.getCause() != null ? e.getCause().getMessage() : "null");
            log.error("❌ Full Stack Trace:", e);
            
            // Try to extract more info from exception
            if (e.getMessage() != null && e.getMessage().contains("signature")) {
                log.error("🔍 SIGNATURE MISMATCH DETECTED!");
                log.error("🔍 This means PayOS response signature doesn't match expected signature");
                log.error("🔍 Possible causes:");
                log.error("   1. Incorrect checksum key in configuration");
                log.error("   2. PayOS API changed response format");
                log.error("   3. Network/proxy modifying response");
                log.error("   4. PayOS server-side issue");
            }
            log.error("========== PAYOS ERROR END ==========");
            
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
        BigDecimal adjustedValue = contractEntity.getAdjustedValue();
        log.info("Supported value for contract {} is {}", contractId, adjustedValue);

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
                log.info("🔍 Transaction type: {}, Issue ID: {}", 
                        transaction.getTransactionType(), transaction.getIssueId());

                // Check if this is a return shipping payment (ORDER_REJECTION)
                boolean isReturnPayment = false;
                if (TransactionEnum.PAID.equals(mappedStatus)) {
                    isReturnPayment = handleReturnShippingPayment(transaction);
                    log.info("📊 Return payment check result: {}", isReturnPayment);
                }

                // Only update contract status if this is NOT a return payment
                // Return payments should not affect contract/order status
                if (!isReturnPayment) {
                    updateContractStatusIfNeeded(transaction);
                } else {
                    log.info("⏩ Skipping contract status update for return payment transaction");
                }
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
                    // Update Order status only (OrderDetail will be updated when assigned to driver)
                    order.setStatus(OrderStatusEnum.FULLY_PAID.name());
                    orderEntityService.save(order);
                } else {
                    log.info("Test2");
                    contract.setStatus(ContractStatusEnum.DEPOSITED.name());
                    // Update Order status only (OrderDetail will be updated when assigned to driver)
                    order.setStatus(OrderStatusEnum.ON_PLANNING.name());
                    orderEntityService.save(order);
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
            var payosTransaction = payOS.paymentRequests().get(Long.valueOf(transaction.getGatewayOrderCode()));

            transaction.setStatus(payosTransaction.getStatus().name());
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
            var refundResponse = payOS.paymentRequests().cancel(Long.parseLong(transaction.getGatewayOrderCode()), reason);

            transaction.setStatus(TransactionEnum.REFUNDED.name());
            transaction.setGatewayResponse(objectMapper.writeValueAsString(refundResponse));

            TransactionEntity updated = transactionEntityService.save(transaction);
            return transactionMapper.toTransactionResponse(updated);

        } catch (Exception e) {
            log.error("Error refunding transaction {}", transactionId, e);
            throw new RuntimeException("Failed to refund transaction", e);
        }
    }

    /**
     * Handle return shipping payment for ORDER_REJECTION issues
     * When customer pays, activate the return journey and notify driver & staff
     * @return true if this is a return payment transaction, false otherwise
     */
    private boolean handleReturnShippingPayment(TransactionEntity transaction) {
        try {
            log.info("🔍 Checking if transaction {} is for return shipping...", transaction.getId());
            log.info("   - Transaction type: {}", transaction.getTransactionType());
            log.info("   - Issue ID: {}", transaction.getIssueId());
            
            // Check if this transaction has an issueId (RETURN_SHIPPING type)
            if (transaction.getIssueId() == null) {
                log.info("⏭️ Transaction {} is not for return shipping (no issueId) - skipping", transaction.getId());
                return false;
            }
            
            // Find issue by ID from transaction
            capstone_project.entity.issue.IssueEntity issue = issueEntityService.findEntityById(transaction.getIssueId())
                    .orElseThrow(() -> new NotFoundException(
                            "Issue not found: " + transaction.getIssueId(),
                            ErrorEnum.NOT_FOUND.getErrorCode()
                    ));
            
            log.info("✅ Found ORDER_REJECTION issue {} with return payment", issue.getId());
            
            // Activate return journey
            if (issue.getReturnJourney() != null) {
                var journey = issue.getReturnJourney();
                journey.setStatus(CommonStatusEnum.ACTIVE.name());
                journeyHistoryEntityService.save(journey);
                log.info("🛣️ Activated return journey: {}", journey.getId());
            }
            
            // Update order details status to RETURNING (customer paid, driver will return these packages)
            if (issue.getOrderDetails() != null && !issue.getOrderDetails().isEmpty()) {
                var affectedOrderDetailIds = issue.getOrderDetails().stream()
                        .map(capstone_project.entity.order.order.OrderDetailEntity::getId)
                        .collect(java.util.stream.Collectors.toSet());
                
                issue.getOrderDetails().forEach(orderDetail -> {
                    orderDetail.setStatus(capstone_project.common.enums.OrderDetailStatusEnum.RETURNING.name());
                    log.info("📦 OrderDetail {} status updated to RETURNING", orderDetail.getTrackingCode());
                });
                orderDetailEntityService.saveAllOrderDetailEntities(issue.getOrderDetails());
                log.info("✅ Updated {} order details to RETURNING status", issue.getOrderDetails().size());
                
                // Update remaining order details in this vehicle assignment to DELIVERED
                // AND update Order status to RETURNING
                var vehicleAssignment = issue.getVehicleAssignmentEntity();
                if (vehicleAssignment != null) {
                    var allOrderDetails = orderDetailEntityService.findByVehicleAssignmentId(vehicleAssignment.getId());
                    var remainingOrderDetails = allOrderDetails.stream()
                            .filter(od -> !affectedOrderDetailIds.contains(od.getId()))
                            .filter(od -> "ONGOING_DELIVERED".equals(od.getStatus()) || "ON_DELIVERED".equals(od.getStatus()))
                            .collect(java.util.stream.Collectors.toList());
                    
                    if (!remainingOrderDetails.isEmpty()) {
                        remainingOrderDetails.forEach(orderDetail -> {
                            orderDetail.setStatus(capstone_project.common.enums.OrderDetailStatusEnum.DELIVERED.name());
                            log.info("📦 OrderDetail {} (not affected) status updated to DELIVERED", orderDetail.getTrackingCode());
                        });
                        orderDetailEntityService.saveAllOrderDetailEntities(remainingOrderDetails);
                        log.info("✅ Updated {} remaining order details to DELIVERED status", remainingOrderDetails.size());
                    }
                    
                    // Update Order status based on ALL OrderDetails using priority logic
                    // CRITICAL: Multi-trip support - check all order details across all trips
                    // Priority: DELIVERED > IN_TROUBLES > RETURNING > RETURNED
                    Optional<OrderEntity> orderOpt = orderEntityService.findVehicleAssignmentOrder(vehicleAssignment.getId());
                    if (orderOpt.isPresent()) {
                        OrderEntity order = orderOpt.get();
                        String oldStatus = order.getStatus();
                        
                        // Get ALL orderDetails of this Order (across all trips)
                        var allDetailsInOrder = orderDetailEntityService.findOrderDetailEntitiesByOrderEntityId(order.getId());
                        
                        long deliveredCount = allDetailsInOrder.stream()
                                .filter(od -> "DELIVERED".equals(od.getStatus())).count();
                        long successfulCount = allDetailsInOrder.stream()
                                .filter(od -> "SUCCESSFUL".equals(od.getStatus())).count();
                        long inTroublesCount = allDetailsInOrder.stream()
                                .filter(od -> "IN_TROUBLES".equals(od.getStatus())).count();
                        long returningCount = allDetailsInOrder.stream()
                                .filter(od -> "RETURNING".equals(od.getStatus())).count();
                        long returnedCount = allDetailsInOrder.stream()
                                .filter(od -> "RETURNED".equals(od.getStatus())).count();
                        long compensationCount = allDetailsInOrder.stream()
                                .filter(od -> "COMPENSATION".equals(od.getStatus())).count();
                        
                        String newStatus;
                        String reason;
                        
                        // Apply priority logic
                        if (deliveredCount > 0 || successfulCount > 0) {
                            // Has delivered packages → SUCCESSFUL
                            newStatus = capstone_project.common.enums.OrderStatusEnum.SUCCESSFUL.name();
                            reason = String.format("%d delivered/successful, %d returning", 
                                    deliveredCount + successfulCount, returningCount);
                        } else if (inTroublesCount > 0) {
                            // No delivered, has troubles → IN_TROUBLES
                            newStatus = capstone_project.common.enums.OrderStatusEnum.IN_TROUBLES.name();
                            reason = String.format("%d in troubles, %d returning", inTroublesCount, returningCount);
                        } else if (returningCount > 0) {
                            // No delivered, no troubles, has returning → RETURNING
                            newStatus = capstone_project.common.enums.OrderStatusEnum.RETURNING.name();
                            reason = String.format("%d packages returning", returningCount);
                        } else if (returnedCount > 0) {
                            // Has returned packages
                            newStatus = capstone_project.common.enums.OrderStatusEnum.RETURNED.name();
                            reason = String.format("%d packages returned", returnedCount);
                        } else if (compensationCount > 0) {
                            newStatus = capstone_project.common.enums.OrderStatusEnum.COMPENSATION.name();
                            reason = String.format("%d compensated", compensationCount);
                        } else {
                            // Fallback
                            newStatus = oldStatus;
                            reason = "No status change needed";
                        }
                        
                        if (!newStatus.equals(oldStatus)) {
                            order.setStatus(newStatus);
                            orderEntityService.save(order);
                            log.info("✅ Order {} status updated from {} to {} (customer paid return shipping, multi-trip: {})", 
                                     order.getId(), oldStatus, newStatus, reason);
                        } else {
                            log.info("ℹ️ Order {} status unchanged: {} ({})", order.getId(), oldStatus, reason);
                        }
                    }
                }
            }
            
            // Update issue status to RESOLVED (customer paid, driver can proceed with return)
            issue.setStatus(IssueEnum.RESOLVED.name());
            issue.setResolvedAt(java.time.LocalDateTime.now());
            issueEntityService.save(issue);
            log.info("✅ Issue {} status updated to RESOLVED", issue.getId());
            
            // Send WebSocket notification to driver
            try {
                var vehicleAssignment = issue.getVehicleAssignmentEntity();
                if (vehicleAssignment != null && vehicleAssignment.getDriver1() != null) {
                    UUID driverId = vehicleAssignment.getDriver1().getUser().getId();
                    UUID returnJourneyId = issue.getReturnJourney() != null 
                            ? issue.getReturnJourney().getId() 
                            : null;
                    
                    // Send via WebSocket to driver
                    issueWebSocketService.sendReturnPaymentSuccessNotification(
                            driverId,
                            issue.getId(),
                            vehicleAssignment.getId(),
                            returnJourneyId
                    );
                    
                    log.info("📢 Sent return payment success notification to driver: {}", driverId);
                }
            } catch (Exception e) {
                log.error("❌ Failed to send driver notification: {}", e.getMessage(), e);
                // Don't throw - notification failure shouldn't break payment processing
            }
            
            // Send WebSocket notification to all staff
            try {
                var vehicleAssignment = issue.getVehicleAssignmentEntity();
                UUID orderId = null;
                String customerName = "N/A";
                String vehicleAssignmentCode = null;
                
                if (vehicleAssignment != null) {
                    vehicleAssignmentCode = vehicleAssignment.getTrackingCode();
                    
                    // Get order from vehicle assignment
                    Optional<OrderEntity> orderOpt = orderEntityService.findVehicleAssignmentOrder(vehicleAssignment.getId());
                    if (orderOpt.isPresent()) {
                        OrderEntity order = orderOpt.get();
                        orderId = order.getId();
                        if (order.getSender() != null && order.getSender().getUser() != null) {
                            customerName = order.getSender().getUser().getFullName();
                        }
                    }
                }
                
                UUID returnJourneyId = issue.getReturnJourney() != null 
                        ? issue.getReturnJourney().getId() 
                        : null;
                
                // Get tracking codes of affected packages
                String trackingCodes = issue.getOrderDetails() != null && !issue.getOrderDetails().isEmpty()
                        ? issue.getOrderDetails().stream()
                                .map(od -> od.getTrackingCode())
                                .collect(java.util.stream.Collectors.joining(", "))
                        : "N/A";
                
                // Get payment amount
                java.math.BigDecimal paymentAmount = transaction.getAmount();
                
                // Send via WebSocket to all staff
                issueWebSocketService.sendReturnPaymentSuccessNotificationToStaff(
                        issue.getId(),
                        orderId,
                        customerName,
                        returnJourneyId,
                        trackingCodes,
                        paymentAmount,
                        vehicleAssignmentCode
                );
                
                log.info("📢 Broadcast return payment success notification to all staff");
            } catch (Exception e) {
                log.error("❌ Failed to send staff notification: {}", e.getMessage(), e);
                // Don't throw - notification failure shouldn't break payment processing
            }
            
            return true; // This is a return payment transaction
            
        } catch (Exception e) {
            log.error("❌ Error handling return shipping payment: {}", e.getMessage(), e);
            // Don't throw - payment is already processed, this is just bonus logic
            return false;
        }
    }

    @Override
    @Transactional
    public TransactionResponse createReturnShippingTransaction(UUID contractId, BigDecimal amount, UUID issueId) {
        log.info("Creating return shipping transaction for contract {} (issueId: {})", contractId, issueId);

        ContractEntity contractEntity = getAndValidateContract(contractId);
        UUID orderId = contractEntity.getOrderEntity() != null ? contractEntity.getOrderEntity().getId() : null;
        
        // Append orderId to returnUrl for proper navigation after payment
        String returnUrl = properties.getReturnUrl() + (orderId != null ? "?orderId=" + orderId : "");
        
        log.info("Creating PayOS PaymentData: cancelUrl={}, returnUrl={}",
                properties.getCancelUrl(), returnUrl);

        Long payOsOrderCode = System.currentTimeMillis();
        
        int finalAmount = amount.setScale(0, RoundingMode.HALF_UP).intValueExact();
        
        log.info("========== PAYOS DEBUG START (RETURN SHIPPING) ==========");
        log.info("🔑 PayOS Credentials Check:");
        log.info("   Client ID: {}", properties.getClientId());
        log.info("   API Key: {}", properties.getApiKey() != null ? "***" + properties.getApiKey().substring(Math.max(0, properties.getApiKey().length() - 4)) : "null");
        log.info("   Checksum Key: {}", properties.getChecksumKey() != null ? "***" + properties.getChecksumKey().substring(Math.max(0, properties.getChecksumKey().length() - 4)) : "null");
        log.info("   Base URL: {}", properties.getBaseUrl());
        
        log.info("📦 PaymentData Fields:");
        log.info("   orderCode: {}", payOsOrderCode);
        log.info("   amount: {} (from BigDecimal: {})", finalAmount, amount);
        log.info("   description: 'Create deposit' (length: {})", "Create deposit".length());
        log.info("   cancelUrl: {}", properties.getCancelUrl());
        log.info("   returnUrl: {}", properties.getReturnUrl());
        log.info("   issueId: {}", issueId);
        log.info("🔄 Trying PayOS SDK 2.0.1 first...");
        
        JsonNode response = null;
        boolean sdkSuccess = false;

        try {
            // Try PayOS SDK 2.0.1 first
            CreatePaymentLinkRequest paymentData = CreatePaymentLinkRequest.builder()
                .orderCode(payOsOrderCode)
                .amount((long) finalAmount)
                .description("Return shipping payment")
                .returnUrl(returnUrl)
                .cancelUrl(properties.getCancelUrl())
                .build();
            
            log.info("📤 Calling PayOS SDK 2.0.1 paymentRequests().create()...");
            CreatePaymentLinkResponse sdkResponse = payOS.paymentRequests().create(paymentData);
            
            // Convert SDK response to JSON
            String responseJson = String.format("""
                {
                  "checkoutUrl": "%s",
                  "paymentLinkId": "%s",
                  "orderCode": %d,
                  "amount": %d,
                  "status": "%s"
                }
                """,
                sdkResponse.getCheckoutUrl(),
                sdkResponse.getPaymentLinkId(),
                sdkResponse.getOrderCode(),
                sdkResponse.getAmount(),
                sdkResponse.getStatus()
            );
            
            response = objectMapper.readValue(responseJson, JsonNode.class);
            sdkSuccess = true;
            log.info("✅ PayOS SDK 2.0.1 SUCCESS!");
            
        } catch (Exception sdkEx) {
            log.warn("⚠️ PayOS SDK 2.0.1 FAILED: {}", sdkEx.getMessage());
            log.info("🔄 Falling back to CustomPayOSClient...");
            
            try {
                // Fallback to CustomPayOSClient
                var customResponse = customPayOSClient.createPaymentLink(
                    payOsOrderCode,
                    finalAmount,
                    "Return shipping payment",
                    returnUrl,
                    properties.getCancelUrl()
                );
                
                // Convert to JSON
                String responseJson = String.format("""
                    {
                      "checkoutUrl": "%s",
                      "paymentLinkId": "%s",
                      "orderCode": %d,
                      "amount": %d,
                      "status": "%s"
                    }
                    """,
                    customResponse.checkoutUrl(),
                    customResponse.paymentLinkId(),
                    customResponse.orderCode(),
                    customResponse.amount(),
                    customResponse.status()
                );
                
                response = objectMapper.readValue(responseJson, JsonNode.class);
                log.info("✅ CustomPayOSClient SUCCESS (Fallback)");
                
            } catch (Exception customEx) {
                log.error("❌ Both SDK and CustomPayOSClient FAILED!");
                throw new RuntimeException("Failed to create payment link", customEx);
            }
        }
        
        try {
            log.info("✅ Payment link created successfully! (Method: {})", sdkSuccess ? "SDK 2.0.1" : "CustomPayOSClient");
            log.info("📥 PayOS response: {}", objectMapper.writeValueAsString(response));
            log.info("========== PAYOS DEBUG END ==========");

            TransactionEntity transaction = TransactionEntity.builder()
                    .id(UUID.randomUUID())
                    .amount(amount)
                    .status(TransactionEnum.PENDING.name())
                    .currencyCode("VND")
                    .paymentProvider("PayOS")
                    .transactionType(capstone_project.common.enums.TransactionTypeEnum.RETURN_SHIPPING.name())
                    .gatewayResponse(objectMapper.writeValueAsString(response))
                    .gatewayOrderCode(String.valueOf(payOsOrderCode))
                    .contractEntity(contractEntity)
                    .issueId(issueId) // Store issue ID for return shipping payment
                    .build();

//            transaction.setStatus(TransactionEnum.DEPOSITED.name());
            TransactionEntity savedEntity = transactionEntityService.save(transaction);
//            contractEntity.setStatus(ContractStatusEnum.DEPOSITED.name());

            // Transaction already has issueId set, no need to link back to issue
            log.info("✅ Created return shipping transaction {} for issue {}", savedEntity.getId(), issueId);

            return transactionMapper.toTransactionResponse(savedEntity);


        } catch (Exception e) {
            log.error("========== PAYOS ERROR (RETURN SHIPPING) ==========");
            log.error("❌ Exception Type: {}", e.getClass().getName());
            log.error("❌ Error Message: {}", e.getMessage());
            log.error("❌ Cause: {}", e.getCause() != null ? e.getCause().getMessage() : "null");
            log.error("❌ Full Stack Trace:", e);
            
            // Try to extract more info from exception
            if (e.getMessage() != null && e.getMessage().contains("signature")) {
                log.error("🔍 SIGNATURE MISMATCH DETECTED!");
                log.error("🔍 This means PayOS response signature doesn't match expected signature");
                log.error("🔍 Possible causes:");
                log.error("   1. Incorrect checksum key in configuration");
                log.error("   2. PayOS API changed response format");
                log.error("   3. Network/proxy modifying response");
                log.error("   4. PayOS server-side issue");
            }
            log.error("========== PAYOS ERROR END ==========");
            
            throw new RuntimeException("Failed to create payment link", e);
        }
    }
}
