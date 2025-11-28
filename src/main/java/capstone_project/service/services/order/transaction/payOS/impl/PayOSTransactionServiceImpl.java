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
import capstone_project.service.services.pricing.PricingUtils;
import capstone_project.service.services.order.order.OrderStatusWebSocketService;
import capstone_project.service.services.notification.NotificationService;
import capstone_project.service.services.notification.NotificationBuilder;
import capstone_project.dtos.request.notification.CreateNotificationRequest;
import capstone_project.repository.entityServices.auth.UserEntityService;
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
@Slf4j
public class PayOSTransactionServiceImpl implements PayOSTransactionService {

    private final TransactionEntityService transactionEntityService;
    private final ContractEntityService contractEntityService;
    private final OrderEntityService orderEntityService;
    private final CustomerEntityService customerEntityService;
    private final UserEntityService userEntityService;
    private final ContractSettingEntityService contractSettingEntityService;
    private final ObjectProvider<OrderService> orderServiceObjectProvider;
    private final NotificationService notificationService;
    
    // ORDER_REJECTION dependencies - Use @Lazy to break circular dependency
    private final capstone_project.repository.entityServices.issue.IssueEntityService issueEntityService;
    private final capstone_project.service.services.issue.IssueService issueService;
    private final capstone_project.repository.entityServices.order.order.JourneyHistoryEntityService journeyHistoryEntityService;
    private final capstone_project.repository.entityServices.order.order.OrderDetailEntityService orderDetailEntityService;
    private final capstone_project.service.services.websocket.IssueWebSocketService issueWebSocketService;
    private final capstone_project.service.services.order.order.OrderStatusWebSocketService orderStatusWebSocketService;
    private final capstone_project.service.services.order.order.OrderDetailStatusWebSocketService orderDetailStatusWebSocketService;
    
    // Payment timeout scheduler to cancel scheduled task when payment received
    private final capstone_project.config.expired.PaymentTimeoutSchedulerService paymentTimeoutSchedulerService;

    private final PayOSProperties properties;
    private final PayOS payOS;
    private final CustomPayOSClient customPayOSClient;

    private final TransactionMapper transactionMapper;
    private final ObjectMapper objectMapper;
    
    // ✅ NEW: OrderDetailStatusService for centralized Order status aggregation
    private final capstone_project.service.services.order.order.OrderDetailStatusService orderDetailStatusService;
    
    // Constructor with @Lazy for IssueService to break circular dependency
    public PayOSTransactionServiceImpl(
            TransactionEntityService transactionEntityService,
            ContractEntityService contractEntityService,
            OrderEntityService orderEntityService,
            CustomerEntityService customerEntityService,
            UserEntityService userEntityService,
            ContractSettingEntityService contractSettingEntityService,
            ObjectProvider<OrderService> orderServiceObjectProvider,
            NotificationService notificationService,
            capstone_project.repository.entityServices.issue.IssueEntityService issueEntityService,
            @org.springframework.context.annotation.Lazy capstone_project.service.services.issue.IssueService issueService,
            capstone_project.repository.entityServices.order.order.JourneyHistoryEntityService journeyHistoryEntityService,
            capstone_project.repository.entityServices.order.order.OrderDetailEntityService orderDetailEntityService,
            capstone_project.service.services.websocket.IssueWebSocketService issueWebSocketService,
            capstone_project.service.services.order.order.OrderStatusWebSocketService orderStatusWebSocketService,
            capstone_project.service.services.order.order.OrderDetailStatusWebSocketService orderDetailStatusWebSocketService,
            capstone_project.config.expired.PaymentTimeoutSchedulerService paymentTimeoutSchedulerService,
            capstone_project.service.services.order.order.OrderDetailStatusService orderDetailStatusService,
            PayOSProperties properties,
            PayOS payOS,
            CustomPayOSClient customPayOSClient,
            TransactionMapper transactionMapper,
            ObjectMapper objectMapper
    ) {
        this.transactionEntityService = transactionEntityService;
        this.contractEntityService = contractEntityService;
        this.orderEntityService = orderEntityService;
        this.customerEntityService = customerEntityService;
        this.userEntityService = userEntityService;
        this.contractSettingEntityService = contractSettingEntityService;
        this.orderServiceObjectProvider = orderServiceObjectProvider;
        this.notificationService = notificationService;
        this.issueEntityService = issueEntityService;
        this.issueService = issueService;
        this.journeyHistoryEntityService = journeyHistoryEntityService;
        this.orderDetailEntityService = orderDetailEntityService;
        this.issueWebSocketService = issueWebSocketService;
        this.orderStatusWebSocketService = orderStatusWebSocketService;
        this.orderDetailStatusWebSocketService = orderDetailStatusWebSocketService;
        this.paymentTimeoutSchedulerService = paymentTimeoutSchedulerService;
        this.orderDetailStatusService = orderDetailStatusService;
        this.properties = properties;
        this.payOS = payOS;
        this.customPayOSClient = customPayOSClient;
        this.transactionMapper = transactionMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public TransactionResponse createTransaction(UUID contractId) {

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

        // Use unified rounding for consistent pricing across all systems
        BigDecimal depositAmount = PricingUtils.calculateRoundedDeposit(totalValue, depositPercent);
        BigDecimal remainingAmount = PricingUtils.calculateRoundedRemaining(totalValue, depositAmount);
        int finalAmount = PricingUtils.roundForDisplay(remainingAmount);
        
        // Append orderId to returnUrl and cancelUrl for proper navigation after payment
        UUID orderId = contractEntity.getOrderEntity() != null ? contractEntity.getOrderEntity().getId() : null;
        String orderCode = contractEntity.getOrderEntity() != null ? contractEntity.getOrderEntity().getOrderCode() : "N/A";
        String returnUrl = properties.getReturnUrl() + (orderId != null ? "?orderId=" + orderId : "");
        String cancelUrl = properties.getCancelUrl() + (orderId != null ? "?orderId=" + orderId : "");
        
        // Create meaningful payment description (max 25 chars for PayOS)
        String desc = String.format("Con lai %s", orderCode);
        String paymentDescription = desc.length() > 25 ? desc.substring(0, 25) : desc;

        JsonNode response = null;
        boolean sdkSuccess = false;

        try {
            // Try PayOS SDK 2.0.1 first
            CreatePaymentLinkRequest paymentData = CreatePaymentLinkRequest.builder()
                .orderCode(payOsOrderCode)
                .amount((long) finalAmount)
                .description(paymentDescription)
                .returnUrl(returnUrl)
                .cancelUrl(cancelUrl)
                .build();

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

        } catch (Exception sdkEx) {
            log.warn("⚠️ PayOS SDK 2.0.1 FAILED: {}", sdkEx.getMessage());

            try {
                // Fallback to CustomPayOSClient
                var customResponse = customPayOSClient.createPaymentLink(
                    payOsOrderCode,
                    finalAmount,
                    paymentDescription,
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

            } catch (Exception customEx) {
                log.error("❌ Both SDK and CustomPayOSClient FAILED!");
                throw new RuntimeException("Failed to create payment link", customEx);
            }
        }
        
        try {

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

        // Use unified rounding for consistent pricing across all systems
        BigDecimal depositAmount = PricingUtils.calculateRoundedDeposit(totalValue, depositPercent);
        int finalAmount = PricingUtils.roundForDisplay(depositAmount);
        
        // Append orderId to returnUrl and cancelUrl for proper navigation after payment
        UUID orderId = contractEntity.getOrderEntity() != null ? contractEntity.getOrderEntity().getId() : null;
        String orderCode = contractEntity.getOrderEntity() != null ? contractEntity.getOrderEntity().getOrderCode() : "N/A";
        String returnUrl = properties.getReturnUrl() + (orderId != null ? "?orderId=" + orderId : "");
        String cancelUrl = properties.getCancelUrl() + (orderId != null ? "?orderId=" + orderId : "");
        
        // Create meaningful payment description (max 25 chars for PayOS)
        String desc = String.format("Dat coc %s", orderCode);
        String paymentDescription = desc.length() > 25 ? desc.substring(0, 25) : desc;

        JsonNode response = null;
        boolean sdkSuccess = false;
        
        try {
            // Try PayOS SDK 2.0.1 first
            CreatePaymentLinkRequest paymentData = CreatePaymentLinkRequest.builder()
                .orderCode(payOsOrderCode)
                .amount((long) finalAmount)
                .description(paymentDescription)
                .returnUrl(returnUrl)
                .cancelUrl(cancelUrl)
                .build();

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

        } catch (Exception sdkEx) {
            log.warn("⚠️ PayOS SDK 2.0.1 FAILED: {}", sdkEx.getMessage());

            try {
                // Fallback to CustomPayOSClient
                var customResponse = customPayOSClient.createPaymentLink(
                    payOsOrderCode,
                    finalAmount,
                    paymentDescription,
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

            } catch (Exception customEx) {
                log.error("❌ Both SDK and CustomPayOSClient FAILED!");
                throw new RuntimeException("Failed to create payment link", customEx);
            }
        }
        
        try {

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

    @Override
    public TransactionResponse getTransactionById(UUID transactionId) {

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
    public List<TransactionResponse> getTransactionsByIssueId(UUID issueId) {

        if (issueId == null) {
            log.error("Issue ID is null");
            throw new BadRequestException(
                    "Issue ID cannot be null",
                    ErrorEnum.INVALID.getErrorCode()
            );
        }

        return transactionEntityService.findByIssueId(issueId).stream()
                .map(transactionMapper::toTransactionResponse)
                .toList();
    }

    @Override
    public GetTransactionStatusResponse getTransactionStatus(UUID transactionId) {

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
        
        try {
            JsonNode webhookEvent = objectMapper.readTree(rawCallbackPayload);

            String orderCode = webhookEvent.path("data").path("orderCode").asText(null);
            String payOsStatus = webhookEvent.path("data").path("status").asText(null);
            String payOsCode = webhookEvent.path("data").path("code").asText(null);

            // Skip test webhook
            if ("123".equals(orderCode)) {
                
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

                // 📧 Send payment notifications based on transaction status
                try {
                    sendPaymentNotification(transaction, mappedStatus);
                } catch (Exception e) {
                    log.error("❌ Failed to send payment notification for transaction {}: {}", 
                        transaction.getId(), e.getMessage());
                    // Don't fail the main flow if notification fails
                }

                // Check if this is a return shipping payment (ORDER_REJECTION)
                boolean isReturnPayment = false;
                if (TransactionEnum.PAID.equals(mappedStatus)) {
                    isReturnPayment = handleReturnShippingPayment(transaction);
                    
                }

                // Only update contract status if this is NOT a return payment
                // Return payments should not affect contract/order status
                if (!isReturnPayment) {
                    updateContractStatusIfNeeded(transaction);
                } else {
                    
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

                if (totalPaidAmount == null) {
                    totalPaidAmount = BigDecimal.ZERO;
                }

                if (totalPaidAmount.compareTo(totalValue) >= 0) {
                    
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
                    
                    // Create payment success notifications
                    createPaymentNotifications(order, contract, totalValue.doubleValue());
                } else {
                    
                    contract.setStatus(ContractStatusEnum.DEPOSITED.name());
                    // Update Order status only (OrderDetail will be updated when assigned to driver)
                    OrderStatusEnum previousStatus = OrderStatusEnum.valueOf(order.getStatus());
                    order.setStatus(OrderStatusEnum.ON_PLANNING.name());
                    orderEntityService.save(order);
                    
                    // Send WebSocket notification
                    orderStatusWebSocketService.sendOrderStatusChange(
                            order.getId(),
                            order.getOrderCode(),
                            previousStatus,
                            OrderStatusEnum.ON_PLANNING
                    );
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
//        
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

            // Check if this transaction has an issueId (RETURN_SHIPPING type)
            if (transaction.getIssueId() == null) {
                
                return false;
            }
            
            // Find issue by ID from transaction
            capstone_project.entity.issue.IssueEntity issue = issueEntityService.findEntityById(transaction.getIssueId())
                    .orElseThrow(() -> new NotFoundException(
                            "Issue not found: " + transaction.getIssueId(),
                            ErrorEnum.NOT_FOUND.getErrorCode()
                    ));

            // ⏰ CRITICAL: Cancel scheduled timeout check (customer paid on time)
            paymentTimeoutSchedulerService.cancelTimeoutCheck(issue.getId());

            // Activate return journey
            if (issue.getReturnJourney() != null) {
                var journey = issue.getReturnJourney();
                journey.setStatus(CommonStatusEnum.ACTIVE.name());
                journeyHistoryEntityService.save(journey);
                
            }
            
            // Update order details status to RETURNING (customer paid, driver will return these packages)
            if (issue.getOrderDetails() != null && !issue.getOrderDetails().isEmpty()) {
                var affectedOrderDetailIds = issue.getOrderDetails().stream()
                        .map(capstone_project.entity.order.order.OrderDetailEntity::getId)
                        .collect(java.util.stream.Collectors.toSet());
                
                UUID vehicleAssignmentId = issue.getVehicleAssignmentEntity() != null ? 
                        issue.getVehicleAssignmentEntity().getId() : null;
                OrderEntity orderForReturning = vehicleAssignmentId != null ? 
                        orderEntityService.findVehicleAssignmentOrder(vehicleAssignmentId).orElse(null) : null;
                
                issue.getOrderDetails().forEach(orderDetail -> {
                    String oldStatus = orderDetail.getStatus();
                    orderDetail.setStatus(capstone_project.common.enums.OrderDetailStatusEnum.RETURNING.name());
                    orderDetailEntityService.save(orderDetail);
                    
                    // Send WebSocket notification
                    if (orderForReturning != null) {
                        try {
                            orderDetailStatusWebSocketService.sendOrderDetailStatusChange(
                                orderDetail.getId(),
                                orderDetail.getTrackingCode(),
                                orderForReturning.getId(),
                                orderForReturning.getOrderCode(),
                                vehicleAssignmentId,
                                oldStatus,
                                capstone_project.common.enums.OrderDetailStatusEnum.RETURNING
                            );
                        } catch (Exception e) {
                            log.error("❌ Failed to send WebSocket for {}: {}", 
                                    orderDetail.getTrackingCode(), e.getMessage());
                        }
                    }

                });

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
                        OrderEntity orderForRemaining = orderEntityService.findVehicleAssignmentOrder(
                                vehicleAssignment.getId()).orElse(null);
                        UUID vaId = vehicleAssignment.getId();
                        
                        remainingOrderDetails.forEach(orderDetail -> {
                            String oldStatus = orderDetail.getStatus();
                            orderDetail.setStatus(capstone_project.common.enums.OrderDetailStatusEnum.DELIVERED.name());
                            orderDetailEntityService.save(orderDetail);
                            
                            // Send WebSocket notification
                            if (orderForRemaining != null) {
                                try {
                                    orderDetailStatusWebSocketService.sendOrderDetailStatusChange(
                                        orderDetail.getId(),
                                        orderDetail.getTrackingCode(),
                                        orderForRemaining.getId(),
                                        orderForRemaining.getOrderCode(),
                                        vaId,
                                        oldStatus,
                                        capstone_project.common.enums.OrderDetailStatusEnum.DELIVERED
                                    );
                                } catch (Exception e) {
                                    log.error("❌ Failed to send WebSocket for {}: {}", 
                                            orderDetail.getTrackingCode(), e.getMessage());
                                }
                            }

                        });
                        
                    }
                    
                    // ✅ CRITICAL FIX: Use OrderDetailStatusService to auto-update Order status
                    // This ensures correct priority logic (COMPENSATION > IN_TROUBLES > CANCELLED > RETURNING/RETURNED > DELIVERED)
                    // NEVER manually calculate Order status - delegate to the centralized service
                    Optional<OrderEntity> orderOpt = orderEntityService.findVehicleAssignmentOrder(vehicleAssignment.getId());
                    if (orderOpt.isPresent()) {
                        OrderEntity order = orderOpt.get();
                        
                        // Trigger auto-update using centralized service
                        // This will apply correct priority logic:
                        // - COMPENSATION (highest priority if ANY package compensated)
                        // - IN_TROUBLES (if ANY package has active issue)
                        // - CANCELLED (if ALL packages cancelled)
                        // - RETURNING/RETURNED (if ALL packages in return flow)
                        // - DELIVERED (only if ALL packages delivered)
                        orderDetailStatusService.triggerOrderStatusUpdate(order.getId());
                        
                        log.info("✅ Order status auto-updated after return payment success for Order: {}", order.getId());
                    }
                }
            }
            
            // ✅ Keep issue in IN_PROGRESS status after payment
            // Issue will only be RESOLVED when driver confirms return delivery
            // No need to update issue status here - driver still needs to physically return the goods

            // Send WebSocket notification to driver
            try {
                var vehicleAssignment = issue.getVehicleAssignmentEntity();
                if (vehicleAssignment != null && vehicleAssignment.getDriver1() != null) {
                    // CRITICAL: Use driver ID (not user ID) to match mobile app subscription
                    UUID driverId = vehicleAssignment.getDriver1().getId();
                    UUID returnJourneyId = issue.getReturnJourney() != null 
                            ? issue.getReturnJourney().getId() 
                            : null;
                    
                    // Get orderId from vehicle assignment
                    UUID orderId = null;
                    Optional<OrderEntity> orderOpt = orderEntityService.findVehicleAssignmentOrder(vehicleAssignment.getId());
                    if (orderOpt.isPresent()) {
                        orderId = orderOpt.get().getId();
                    }
                    
                    // Send via WebSocket to driver
                    issueWebSocketService.sendReturnPaymentSuccessNotification(
                            driverId,
                            issue.getId(),
                            vehicleAssignment.getId(),
                            returnJourneyId,
                            orderId
                    );

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

                // CRITICAL: Broadcast issue update so staff UI can refetch issue detail
                // This allows staff to see updated transaction status immediately
                try {
                    // Fetch updated issue with transaction
                    capstone_project.dtos.response.issue.GetBasicIssueResponse updatedIssueResponse = 
                        issueService.getBasicIssue(issue.getId());
                    
                    // Broadcast to all subscribed clients (including staff viewing issue detail)
                    issueWebSocketService.broadcastIssueStatusChange(updatedIssueResponse);

                } catch (Exception broadcastEx) {
                    log.error("❌ Failed to broadcast issue update: {}", broadcastEx.getMessage(), broadcastEx);
                }
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

        // CRITICAL: Return shipping payment is INDEPENDENT from contract payment
        // Contract is already PAID - this is a NEW payment for return shipping cost
        // DO NOT validate contract status here
        ContractEntity contractEntity = contractEntityService.findEntityById(contractId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));
        
        // Validate customer is active
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
        
        UUID orderId = contractEntity.getOrderEntity() != null ? contractEntity.getOrderEntity().getId() : null;
        
        // Get issue details for meaningful description (max 25 chars for PayOS)
        String paymentDescription = "Tra hang";
        if (issueId != null) {
            try {
                capstone_project.entity.issue.IssueEntity issue = issueEntityService.findEntityById(issueId)
                        .orElse(null);
                if (issue != null && issue.getVehicleAssignmentEntity() != null) {
                    String trackingCode = issue.getVehicleAssignmentEntity().getTrackingCode();
                    
                    // Get affected order details count from issue relationship
                    List<capstone_project.entity.order.order.OrderDetailEntity> affectedOrderDetails = 
                            issue.getOrderDetails();
                    
                    if (affectedOrderDetails != null && !affectedOrderDetails.isEmpty()) {
                        int packageCount = affectedOrderDetails.size();
                        // Format: "Tra hang TC123 (3k)" - ensure max 25 chars
                        String desc = String.format("Tra hang %s (%dk)", trackingCode, packageCount);
                        paymentDescription = desc.length() > 25 ? desc.substring(0, 25) : desc;
                    } else {
                        // Format: "Tra hang TC123" - ensure max 25 chars
                        String desc = String.format("Tra hang %s", trackingCode);
                        paymentDescription = desc.length() > 25 ? desc.substring(0, 25) : desc;
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to get issue details for payment description: {}", e.getMessage());
            }
        }
        
        // Append orderId to returnUrl for proper navigation after payment
        String returnUrl = properties.getReturnUrl() + (orderId != null ? "?orderId=" + orderId : "");

        Long payOsOrderCode = System.currentTimeMillis();
        
        // Use unified rounding for consistent pricing across all systems
        int finalAmount = PricingUtils.roundForDisplay(amount);

        JsonNode response = null;
        boolean sdkSuccess = false;

        try {
            // Try PayOS SDK 2.0.1 first
            CreatePaymentLinkRequest paymentData = CreatePaymentLinkRequest.builder()
                .orderCode(payOsOrderCode)
                .amount((long) finalAmount)
                .description(paymentDescription)
                .returnUrl(returnUrl)
                .cancelUrl(properties.getCancelUrl())
                .build();

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

        } catch (Exception sdkEx) {
            log.warn("⚠️ PayOS SDK 2.0.1 FAILED: {}", sdkEx.getMessage());

            try {
                // Fallback to CustomPayOSClient
                var customResponse = customPayOSClient.createPaymentLink(
                    payOsOrderCode,
                    finalAmount,
                    paymentDescription,
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

            } catch (Exception customEx) {
                log.error("❌ Both SDK and CustomPayOSClient FAILED!");
                throw new RuntimeException("Failed to create payment link", customEx);
            }
        }
        
        try {

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
    
    /**
     * Helper method to send payment notifications based on transaction status
     */
    private void sendPaymentNotification(TransactionEntity transaction, TransactionEnum status) {
        try {
            ContractEntity contract = transaction.getContractEntity();
            if (contract == null || contract.getOrderEntity() == null) {
                log.warn("Cannot send payment notification: no contract or order found for transaction {}", 
                    transaction.getId());
                return;
            }
            
            OrderEntity order = contract.getOrderEntity();
            CustomerEntity customer = order.getSender();
            
            if (customer == null) {
                log.warn("Cannot send payment notification: no customer found for order {}", order.getId());
                return;
            }
            
            switch (status) {
                case PAID:
                    // Customer notification: Payment successful
                    CreateNotificationRequest customerNotification = CreateNotificationRequest.builder()
                        .userId(customer.getId())
                        .recipientRole("CUSTOMER")
                        .title("Thanh toán thành công")
                        .description("Đơn hàng " + order.getOrderCode() + " đã được thanh toán thành công với số tiền " + 
                            transaction.getAmount() + " VNĐ")
                        .notificationType(capstone_project.common.enums.NotificationTypeEnum.PAYMENT_FULL_SUCCESS)
                        .relatedOrderId(order.getId())
                        .relatedContractId(contract.getId())
                        .build();
                    
                    notificationService.createNotification(customerNotification);
                    
                    // Staff notification: Payment received
                    sendPaymentNotificationToStaff(order, transaction, "Thanh toán mới nhận được", 
                        "Đơn hàng " + order.getOrderCode() + " đã thanh toán thành công");
                    
                    log.info("📧 Payment success notifications sent for transaction {}", transaction.getId());
                    break;
                    
                case CANCELLED:
                    // Customer notification: Payment cancelled
                    CreateNotificationRequest cancelledNotification = CreateNotificationRequest.builder()
                        .userId(customer.getId())
                        .recipientRole("CUSTOMER")
                        .title("Thanh toán đã hủy")
                        .description("Thanh toán cho đơn hàng " + order.getOrderCode() + " đã bị hủy")
                        .notificationType(capstone_project.common.enums.NotificationTypeEnum.ORDER_CANCELLED)
                        .relatedOrderId(order.getId())
                        .relatedContractId(contract.getId())
                        .build();
                    
                    notificationService.createNotification(cancelledNotification);
                    
                    // Staff notification: Payment cancelled
                    sendPaymentNotificationToStaff(order, transaction, "Thanh toán bị hủy", 
                        "Thanh toán cho đơn hàng " + order.getOrderCode() + " đã bị hủy");
                    
                    log.info("📧 Payment cancelled notifications sent for transaction {}", transaction.getId());
                    break;
                    
                case FAILED:
                    // Customer notification: Payment failed
                    CreateNotificationRequest failedNotification = CreateNotificationRequest.builder()
                        .userId(customer.getId())
                        .recipientRole("CUSTOMER")
                        .title("Thanh toán thất bại")
                        .description("Thanh toán cho đơn hàng " + order.getOrderCode() + " đã thất bại. Vui lòng thử lại.")
                        .notificationType(capstone_project.common.enums.NotificationTypeEnum.ORDER_CANCELLED)
                        .relatedOrderId(order.getId())
                        .relatedContractId(contract.getId())
                        .build();
                    
                    notificationService.createNotification(failedNotification);
                    
                    // Staff notification: Payment failed
                    sendPaymentNotificationToStaff(order, transaction, "Thanh toán thất bại", 
                        "Thanh toán cho đơn hàng " + order.getOrderCode() + " đã thất bại");
                    
                    log.info("📧 Payment failed notifications sent for transaction {}", transaction.getId());
                    break;
                    
                case REFUNDED:
                    // Customer notification: Refund processed
                    CreateNotificationRequest refundNotification = CreateNotificationRequest.builder()
                        .userId(customer.getId())
                        .recipientRole("CUSTOMER")
                        .title("Hoàn tiền thành công")
                        .description("Đã hoàn tiền " + transaction.getAmount() + " VNĐ cho đơn hàng " + order.getOrderCode())
                        .notificationType(capstone_project.common.enums.NotificationTypeEnum.PAYMENT_RECEIVED)
                        .relatedOrderId(order.getId())
                        .relatedContractId(contract.getId())
                        .build();
                    
                    notificationService.createNotification(refundNotification);
                    
                    // Staff notification: Refund processed
                    sendPaymentNotificationToStaff(order, transaction, "Hoàn tiền đã xử lý", 
                        "Đã hoàn tiền cho đơn hàng " + order.getOrderCode());
                    
                    log.info("📧 Refund notifications sent for transaction {}", transaction.getId());
                    break;
                    
                default:
                    // Other statuses (PENDING, EXPIRED) may not need notifications
                    break;
            }
        } catch (Exception e) {
            log.error("❌ Failed to send payment notification for transaction {}: {}", 
                transaction.getId(), e.getMessage());
            // Don't throw - Notification failure shouldn't break business logic
        }
    }
    
    /**
     * Helper method to send payment notifications to all staff users
     */
    private void sendPaymentNotificationToStaff(OrderEntity order, TransactionEntity transaction, 
            String title, String description) {
        try {
            var staffUsers = userEntityService.getUserEntitiesByRoleRoleName("STAFF");
            if (!staffUsers.isEmpty()) {
                for (var staff : staffUsers) {
                    CreateNotificationRequest staffNotification = CreateNotificationRequest.builder()
                        .userId(staff.getId())
                        .recipientRole("STAFF")
                        .title(title)
                        .description(description + " (Số tiền: " + transaction.getAmount() + " VNĐ)")
                        .notificationType(capstone_project.common.enums.NotificationTypeEnum.PAYMENT_RECEIVED)
                        .relatedOrderId(order.getId())
                        .relatedContractId(transaction.getContractEntity() != null ? transaction.getContractEntity().getId() : null)
                        .build();
                    
                    notificationService.createNotification(staffNotification);
                }
                log.info("📧 Payment notification sent to {} staff users for order {}", 
                    staffUsers.size(), order.getOrderCode());
            }
        } catch (Exception e) {
            log.error("❌ Failed to send payment notification to staff for order {}: {}", 
                order.getOrderCode(), e.getMessage());
            // Don't throw - Notification failure shouldn't break business logic
        }
    }
    
    /**
     * Create payment success notifications for customer and driver
     */
    private void createPaymentNotifications(OrderEntity order, ContractEntity contract, double totalAmount) {
        try {
            CustomerEntity customer = order.getSender();
            if (customer == null || customer.getUser() == null) {
                log.warn("Cannot find customer for order {}", order.getOrderCode());
                return;
            }
            
            String contractCode = contract.getContractName() != null ? 
                contract.getContractName() : "HĐ-" + order.getOrderCode();
            
            // Notification 1: To Customer - PAYMENT_FULL_SUCCESS
            try {
                CreateNotificationRequest customerNotif = NotificationBuilder.buildPaymentFullSuccess(
                    customer.getUser().getId(),
                    order.getOrderCode(),
                    contractCode,
                    totalAmount,
                    order.getId(),
                    contract.getId()
                );
                
                notificationService.createNotification(customerNotif);
                log.info("✅ Created PAYMENT_FULL_SUCCESS notification for order: {}", order.getOrderCode());
            } catch (Exception e) {
                log.error("❌ Failed to create PAYMENT_FULL_SUCCESS notification: {}", e.getMessage());
            }
            
            // Notification 2: To Driver - PAYMENT_RECEIVED (if driver assigned)
            try {
                // Get order details from order entity
                List<capstone_project.entity.order.order.OrderDetailEntity> orderDetails = 
                    order.getOrderDetailEntities();
                
                if (orderDetails != null && !orderDetails.isEmpty()) {
                    capstone_project.entity.vehicle.VehicleAssignmentEntity assignment = 
                        orderDetails.get(0).getVehicleAssignmentEntity();
                    
                    if (assignment != null && assignment.getDriver1() != null) {
                        capstone_project.entity.user.driver.DriverEntity driver = assignment.getDriver1();
                        
                        CreateNotificationRequest driverNotif = NotificationBuilder.buildPaymentReceived(
                            driver.getUser().getId(),
                            order.getOrderCode(),
                            totalAmount,
                            customer.getUser().getFullName(),
                            customer.getUser().getPhoneNumber(),
                            order.getId(),
                            contract.getId()
                        );
                        
                        notificationService.createNotification(driverNotif);
                        log.info("✅ Created PAYMENT_RECEIVED notification for driver: {}", 
                            driver.getUser().getFullName());
                    }
                }
            } catch (Exception e) {
                log.error("❌ Failed to create PAYMENT_RECEIVED notification: {}", e.getMessage());
            }
            
        } catch (Exception e) {
            log.error("❌ Failed to create payment notifications: {}", e.getMessage());
        }
    }
}
