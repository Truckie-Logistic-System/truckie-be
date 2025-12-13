package capstone_project.service.services.order.transaction.payOS.impl;

import capstone_project.common.enums.*;
import capstone_project.common.exceptions.dto.BadRequestException;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.config.payment.PayOS.PayOSProperties;
import capstone_project.dtos.response.order.transaction.GetTransactionStatusResponse;
import capstone_project.dtos.response.order.transaction.TransactionResponse;
import capstone_project.dtos.response.order.CreateOrderResponse;
import capstone_project.entity.auth.UserEntity;
import capstone_project.entity.order.contract.ContractEntity;
import capstone_project.entity.order.order.OrderEntity;
import capstone_project.entity.order.order.OrderDetailEntity;
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
import capstone_project.service.services.vehicle.VehicleReservationService;
import capstone_project.service.services.order.order.OrderStatusWebSocketService;
import capstone_project.service.services.notification.NotificationService;
import capstone_project.service.services.notification.NotificationBuilder;
import capstone_project.dtos.request.notification.CreateNotificationRequest;
import capstone_project.service.events.payment.DepositPaidEvent;
import capstone_project.service.events.payment.FullPaymentPaidEvent;
import capstone_project.repository.entityServices.auth.UserEntityService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
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
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class PayOSTransactionServiceImpl implements PayOSTransactionService {

    // 🔒 Deduplication lock to prevent duplicate webhook processing
    private static final ConcurrentHashMap<String, Long> processingWebhooks = new ConcurrentHashMap<>();
    private static final long WEBHOOK_LOCK_TIMEOUT_MS = 30000; // 30 seconds

    private final TransactionEntityService transactionEntityService;
    private final ContractEntityService contractEntityService;
    private final OrderEntityService orderEntityService;
    private final CustomerEntityService customerEntityService;
    private final UserEntityService userEntityService;
    private final ContractSettingEntityService contractSettingEntityService;
    private final ObjectProvider<OrderService> orderServiceObjectProvider;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher applicationEventPublisher;
    
    // ORDER_REJECTION dependencies - Use @Lazy to break circular dependency
    private final capstone_project.repository.entityServices.issue.IssueEntityService issueEntityService;
    private final capstone_project.service.services.issue.IssueService issueService;
    private final capstone_project.repository.entityServices.order.order.JourneyHistoryEntityService journeyHistoryEntityService;
    private final capstone_project.repository.entityServices.order.order.OrderDetailEntityService orderDetailEntityService;
    private final capstone_project.service.services.websocket.IssueWebSocketService issueWebSocketService;
    private final capstone_project.service.services.order.order.OrderStatusWebSocketService orderStatusWebSocketService;
    private final capstone_project.service.services.order.order.OrderDetailStatusWebSocketService orderDetailStatusWebSocketService;
    private final VehicleReservationService vehicleReservationService;
    
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
            ApplicationEventPublisher applicationEventPublisher,
            capstone_project.repository.entityServices.issue.IssueEntityService issueEntityService,
            @org.springframework.context.annotation.Lazy capstone_project.service.services.issue.IssueService issueService,
            capstone_project.repository.entityServices.order.order.JourneyHistoryEntityService journeyHistoryEntityService,
            capstone_project.repository.entityServices.order.order.OrderDetailEntityService orderDetailEntityService,
            capstone_project.service.services.websocket.IssueWebSocketService issueWebSocketService,
            capstone_project.service.services.order.order.OrderStatusWebSocketService orderStatusWebSocketService,
            capstone_project.service.services.order.order.OrderDetailStatusWebSocketService orderDetailStatusWebSocketService,
            VehicleReservationService vehicleReservationService,
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
        this.applicationEventPublisher = applicationEventPublisher;
        this.issueEntityService = issueEntityService;
        this.issueService = issueService;
        this.journeyHistoryEntityService = journeyHistoryEntityService;
        this.orderDetailEntityService = orderDetailEntityService;
        this.issueWebSocketService = issueWebSocketService;
        this.orderStatusWebSocketService = orderStatusWebSocketService;
        this.orderDetailStatusWebSocketService = orderDetailStatusWebSocketService;
        this.vehicleReservationService = vehicleReservationService;
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

        // Get deposit percent: prioritize contract's custom value, fallback to global setting
        BigDecimal depositPercent = getEffectiveDepositPercent(contractEntity);
        log.info("📊 Using deposit percent: {}% (custom: {})", depositPercent, 
            contractEntity.getCustomDepositPercent() != null ? "yes" : "no");

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

        // Get deposit percent: prioritize contract's custom value, fallback to global setting
        BigDecimal depositPercent = getEffectiveDepositPercent(contractEntity);
        log.info("📊 Creating deposit transaction with percent: {}% (custom: {})", depositPercent, 
            contractEntity.getCustomDepositPercent() != null ? "yes" : "no");

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
    public void handleWebhook(String rawCallbackPayload) {
        
        log.info("🔔 PayOS Webhook received - Raw payload: {}", rawCallbackPayload);
        
        String parsedOrderCode = null;
        try {
            JsonNode webhookEvent = objectMapper.readTree(rawCallbackPayload);

            parsedOrderCode = webhookEvent.path("data").path("orderCode").asText(null);
            final String orderCode = parsedOrderCode;
            String payOsStatus = webhookEvent.path("data").path("status").asText(null);
            String payOsCode = webhookEvent.path("data").path("code").asText(null);

            log.info("\uD83D\uDCE5 [PayOS Webhook] Received callback: orderCode={}, payOsStatus={}, payOsCode={}",
                    orderCode, payOsStatus, payOsCode);

            log.info("🔍 Webhook parsed - orderCode: {}, status: {}, code: {}", orderCode, payOsStatus, payOsCode);

            // Skip test webhook
            if ("123".equals(orderCode)) {
                log.info("⏭️ Skipping test webhook");
                return;
            }

            if (orderCode == null) {
                log.error("Invalid webhook payload: {}", rawCallbackPayload);
                return;
            }

            // 🔒 DEDUPLICATION: Check if this orderCode is already being processed
            Long existingTimestamp = processingWebhooks.get(orderCode);
            long currentTime = System.currentTimeMillis();
            if (existingTimestamp != null && (currentTime - existingTimestamp) < WEBHOOK_LOCK_TIMEOUT_MS) {
                log.info("⏭️ Webhook for orderCode {} is already being processed, skipping duplicate", orderCode);
                return;
            }
            
            // Mark this orderCode as being processed
            processingWebhooks.put(orderCode, currentTime);
            
            try {
                transactionEntityService.findByGatewayOrderCode(orderCode).ifPresentOrElse(transaction -> {
                TransactionEnum mappedStatus = mapPayOsStatusToEnum(payOsStatus, payOsCode);
                
                log.info("\uD83D\uDD01 [PayOS Webhook] Processing transaction {} - currentStatus={}, mappedStatus={}",
                        transaction.getId(), transaction.getStatus(), mappedStatus.name());

                // 🔍 DEDUPLICATION: Skip if transaction already has the target status
                // Re-fetch from database to get latest status (avoid race condition)
                TransactionEntity freshTransaction = transactionEntityService.findEntityById(transaction.getId()).orElse(null);
                if (freshTransaction != null && mappedStatus.name().equals(freshTransaction.getStatus())) {
                    log.info("⏭️ Transaction {} already has status {}, skipping duplicate processing", 
                        transaction.getId(), mappedStatus.name());
                    return;
                }
                
                // Update transaction status in separate transaction
                updateTransactionStatus(transaction, mappedStatus, rawCallbackPayload);
                
                log.info("✅ Transaction {} updated to status: {}", transaction.getId(), mappedStatus.name());

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
                    log.info("📋 Updating contract status for transaction: {}", transaction.getId());
                    updateContractStatusIfNeeded(transaction);
                } else {
                    log.info("⏭️ Skipping contract status update for return payment: {}", transaction.getId());
                }
            }, () -> {
                log.warn("❌ Transaction not found for orderCode {}", orderCode);
            });
            } finally {
                // Clean up the lock after processing
                processingWebhooks.remove(orderCode);
            }

        } catch (Exception e) {
            log.error("Failed to handle webhook", e);
            // Clean up on error
            if (parsedOrderCode != null) {
                processingWebhooks.remove(parsedOrderCode);
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateTransactionStatus(TransactionEntity transaction, TransactionEnum status, String rawCallbackPayload) {
        transaction.setStatus(status.name());
        transaction.setGatewayResponse(rawCallbackPayload);
        transaction.setPaymentDate(java.time.LocalDateTime.now());
        transactionEntityService.save(transaction);
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
        log.info("📋 Starting contract status update for transaction: {}", transaction.getId());
        
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

        log.info("🔍 Current statuses - Contract: {}, Order: {}, Transaction: {}", 
            contract.getStatus(), order.getStatus(), transaction.getStatus());

        switch (TransactionEnum.valueOf(transaction.getStatus())) {
            case PAID -> {
                log.info("🔍 Processing PAID transaction: {}", transaction.getId());
                
                BigDecimal totalValue = validationTotalValue(contract.getId());
                BigDecimal totalPaidAmount = transactionEntityService.sumPaidAmountByContractId(contract.getId());

                if (totalPaidAmount == null) {
                    totalPaidAmount = BigDecimal.ZERO;
                }

                log.info("💰 Payment calculation - totalPaid: {}, totalValue: {}, comparison: {}", 
                    totalPaidAmount, totalValue, totalPaidAmount.compareTo(totalValue));

                if (totalPaidAmount.compareTo(totalValue) >= 0) {
                    log.info("📋 Processing FULL payment scenario");
                    
                    // Update contract and order status
                    contract.setStatus(ContractStatusEnum.PAID.name());
                    OrderStatusEnum previousStatus = OrderStatusEnum.valueOf(order.getStatus());
                    order.setStatus(OrderStatusEnum.FULLY_PAID.name());
                    orderEntityService.save(order);
                    contractEntityService.save(contract);
                    
                    log.info("✅ Order {} status updated from {} to {}", 
                        order.getOrderCode(), previousStatus, OrderStatusEnum.FULLY_PAID);
                    
                    // Send WebSocket notification
                    orderStatusWebSocketService.sendOrderStatusChange(
                            order.getId(),
                            order.getOrderCode(),
                            previousStatus,
                            OrderStatusEnum.FULLY_PAID
                    );
                    
                    log.info("📡 WebSocket notification sent for full payment");
                    
                } else {
                    log.info("💰 Processing DEPOSIT payment scenario");
                    
                    // Update contract status
                    contract.setStatus(ContractStatusEnum.DEPOSITED.name());
                    contractEntityService.save(contract);
                    
                    // Update both order and all order details status to ON_PLANNING
                    CreateOrderResponse response = orderService.changeStatusOrderWithAllOrderDetail(order.getId(), OrderStatusEnum.ON_PLANNING);
                    
                    log.info("✅ Order {} and all order details status updated to ON_PLANNING", 
                        order.getOrderCode());
                    
                    log.info("📡 WebSocket notification sent for deposit payment");
                }
            }

            case CANCELLED, EXPIRED, FAILED -> {
                contract.setStatus(ContractStatusEnum.UNPAID.name());
                
                // Cancel vehicle reservations when payment fails
                try {
                    vehicleReservationService.cancelReservationsForOrder(order.getId());
                    log.info("✅ Cancelled vehicle reservations for order {} due to payment failure: {}", 
                            order.getId(), transaction.getStatus());
                } catch (Exception e) {
                    log.error("❌ Failed to cancel reservations for order {}: {}", order.getId(), e.getMessage());
                    // Don't throw - reservation cancel failure shouldn't break payment processing
                }
            }

            case REFUNDED -> {
                contract.setStatus(ContractStatusEnum.REFUNDED.name());
                
                // Cancel vehicle reservations when refunded
                try {
                    vehicleReservationService.cancelReservationsForOrder(order.getId());
                    log.info("✅ Cancelled vehicle reservations for order {} due to refund", order.getId());
                } catch (Exception e) {
                    log.error("❌ Failed to cancel reservations for order {}: {}", order.getId(), e.getMessage());
                    // Don't throw - reservation cancel failure shouldn't break refund processing
                }

                // ✅ IMPORTANT: Do NOT set Order status to RETURNED directly here.
                // RETURNING/RETURNED are max statuses and must be derived via aggregation
                // over all OrderDetails (OrderDetailStatusService).
                try {
                    orderDetailStatusService.triggerOrderStatusUpdate(order.getId());
                    log.info("✅ Triggered aggregated Order status update after REFUNDED payment for order {}", order.getId());
                } catch (Exception e) {
                    log.error("❌ Failed to trigger aggregated Order status update after REFUNDED payment for order {}: {}",
                            order.getId(), e.getMessage());
                    // Don't throw - aggregation failure shouldn't break refund processing
                }
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

            log.info("\uD83D\uDD0E [handleReturnShippingPayment] Start - transactionId={}, status={}, type={}, amount={}, issueId={}",
                    transaction.getId(),
                    transaction.getStatus(),
                    transaction.getTransactionType(),
                    transaction.getAmount(),
                    transaction.getIssueId());

            // Check if this transaction has an issueId (RETURN_SHIPPING type)
            if (transaction.getIssueId() == null) {
                log.warn("⚠️ [handleReturnShippingPayment] Transaction {} has no issueId, skipping return shipping flow", transaction.getId());
                return false;
            }
            
            // Find issue by ID from transaction - MUST use findByIdWithDetails to fetch LAZY relationships
            // (vehicleAssignmentEntity, driver1, etc.) otherwise they will be null
            capstone_project.entity.issue.IssueEntity issue = issueEntityService.findByIdWithDetails(transaction.getIssueId())
                    .orElseThrow(() -> new NotFoundException(
                            "Issue not found: " + transaction.getIssueId(),
                            ErrorEnum.NOT_FOUND.getErrorCode()
                    ));
            
            log.info("🔍 [handleReturnShippingPayment] Issue loaded with details - ID: {}", issue.getId());

            // ⏰ CRITICAL: Cancel scheduled timeout check (customer paid on time)
            paymentTimeoutSchedulerService.cancelTimeoutCheck(issue.getId());

            // ✅ Safety: Clear deadline after payment so safety-net scheduler cannot timeout a paid issue
            // Issue may remain IN_PROGRESS until driver completes return delivery, but payment is settled.
            try {
                issue.setPaymentDeadline(null);
                issueEntityService.save(issue);
            } catch (Exception e) {
                log.warn("⚠️ Failed to clear payment deadline for paid issue {}: {}", issue.getId(), e.getMessage());
            }

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

                // 📧 Send notifications for RETURNING status (customer paid, packages being returned)
                try {
                    if (orderForReturning != null && orderForReturning.getSender() != null && orderForReturning.getSender().getUser() != null) {
                        // Get all order details for context
                        List<OrderDetailEntity> allOrderDetails = orderDetailEntityService.findOrderDetailEntitiesByOrderEntityId(orderForReturning.getId());
                        int returningCount = issue.getOrderDetails().size();
                        int totalPackageCount = allOrderDetails.size();
                        
                        // Customer notification: RETURNING with email
                        var customerNotification = capstone_project.service.services.notification.NotificationBuilder.buildCustomerReturnInProgress(
                            orderForReturning.getSender().getUser().getId(),
                            orderForReturning.getOrderCode(),
                            returningCount,
                            totalPackageCount,
                            issue.getOrderDetails(),
                            orderForReturning.getId(),
                            issue.getOrderDetails().stream().map(OrderDetailEntity::getId).collect(java.util.stream.Collectors.toList())
                        );
                        notificationService.createNotification(customerNotification);
                        log.info("📧 Customer notification created for RETURNING status ({} / {} packages)",
                                returningCount, totalPackageCount);
                        
                        // Staff notification: RETURNING without email
                        var staffNotificationTemplate = capstone_project.service.services.notification.NotificationBuilder.buildStaffReturnInProgress(
                            null, // Will be set for each staff user
                            orderForReturning.getOrderCode(),
                            orderForReturning.getSender().getUser().getFullName(),
                            returningCount,
                            totalPackageCount,
                            issue.getOrderDetails(),
                            orderForReturning.getId(),
                            issue.getOrderDetails().stream().map(OrderDetailEntity::getId).collect(java.util.stream.Collectors.toList())
                        );
                        sendStaffNotification(staffNotificationTemplate);
                        log.info("📧 Staff notification created for RETURNING status ({} / {} packages)",
                                returningCount, totalPackageCount);
                    }
                } catch (Exception e) {
                    log.error("❌ Failed to create notifications for RETURNING status", e);
                    // Don't fail the main flow if notification fails
                }

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
                log.info("🔍 [DRIVER_NOTIFICATION] Checking vehicleAssignment: {}", vehicleAssignment != null ? vehicleAssignment.getId() : "NULL");
                
                if (vehicleAssignment != null) {
                    log.info("🔍 [DRIVER_NOTIFICATION] Driver1: {}", vehicleAssignment.getDriver1() != null ? vehicleAssignment.getDriver1().getId() : "NULL");
                }
                
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
                    
                    log.info("📤 [DRIVER_NOTIFICATION] Sending RETURN_PAYMENT_SUCCESS to driver: {}", driverId);
                    
                    // Send via WebSocket to driver
                    issueWebSocketService.sendReturnPaymentSuccessNotification(
                            driverId,
                            issue.getId(),
                            vehicleAssignment.getId(),
                            returnJourneyId,
                            orderId
                    );
                    
                    log.info("✅ [DRIVER_NOTIFICATION] Successfully called sendReturnPaymentSuccessNotification");

                } else {
                    log.warn("⚠️ [DRIVER_NOTIFICATION] Cannot send notification - vehicleAssignment or driver1 is NULL! issueId={}, transactionId={}",
                            issue.getId(), transaction.getId());
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
                        
                        // Return shipping notification is handled by sendPaymentNotification method
                        log.info("✅ Return shipping payment processed for order: {}", order.getOrderCode());
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
            
            // Create STAFF_RETURN_PAYMENT persistent notification
            try {
                var vehicleAssignment = issue.getVehicleAssignmentEntity();
                UUID staffOrderId = null;
                String staffCustomerName = "N/A";
                int returnPackageCount = issue.getOrderDetails() != null ? issue.getOrderDetails().size() : 0;
                double returnFee = transaction.getAmount() != null ? transaction.getAmount().doubleValue() : 0.0;
                
                if (vehicleAssignment != null) {
                    Optional<OrderEntity> staffOrderOpt = orderEntityService.findVehicleAssignmentOrder(vehicleAssignment.getId());
                    if (staffOrderOpt.isPresent()) {
                        OrderEntity staffOrder = staffOrderOpt.get();
                        staffOrderId = staffOrder.getId();
                        if (staffOrder.getSender() != null) {
                            staffCustomerName = staffOrder.getSender().getRepresentativeName() != null ?
                                staffOrder.getSender().getRepresentativeName() : staffOrder.getSender().getUser().getUsername();
                        }
                        
                        var staffUsers = userEntityService.getUserEntitiesByRoleRoleName("STAFF");
                        for (var staff : staffUsers) {
                            CreateNotificationRequest staffNotif = NotificationBuilder.buildStaffReturnPayment(
                                staff.getId(),
                                staffOrder.getOrderCode(),
                                returnFee,
                                staffCustomerName,
                                returnPackageCount,
                                staffOrderId,
                                issue.getId()
                            );
                            notificationService.createNotification(staffNotif);
                        }
                        log.info("✅ Created STAFF_RETURN_PAYMENT notifications for {} staff users", staffUsers.size());
                    }
                }
            } catch (Exception e) {
                log.error("❌ Failed to create STAFF_RETURN_PAYMENT notification: {}", e.getMessage());
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
                    // Determine notification type based on transaction type
                    String transactionType = transaction.getTransactionType();
                    capstone_project.common.enums.NotificationTypeEnum notificationType;
                    String title;
                    String description;
                    
                    if ("DEPOSIT".equals(transactionType)) {
                        notificationType = capstone_project.common.enums.NotificationTypeEnum.PAYMENT_DEPOSIT_SUCCESS;
                        title = "Thanh toán cọc thành công";
                        description = "Đơn hàng " + order.getOrderCode() + " đã thanh toán cọc thành công với số tiền " + 
                            transaction.getAmount() + " VNĐ";
                        log.info("💰 Processing DEPOSIT payment notification for transaction: {}", transaction.getId());
                    } else if ("FULL_PAYMENT".equals(transactionType)) {
                        notificationType = capstone_project.common.enums.NotificationTypeEnum.PAYMENT_FULL_SUCCESS;
                        title = "Thanh toán đầy đủ thành công";
                        description = "Đơn hàng " + order.getOrderCode() + " đã thanh toán đầy đủ thành công với số tiền " + 
                            transaction.getAmount() + " VNĐ";
                        log.info("💰 Processing FULL_PAYMENT notification for transaction: {}", transaction.getId());
                    } else if ("RETURN_SHIPPING".equals(transactionType)) {
                        notificationType = capstone_project.common.enums.NotificationTypeEnum.RETURN_PAYMENT_SUCCESS;
                        title = "Thanh toán cước trả hàng thành công";
                        description = "Đơn hàng " + order.getOrderCode() + " đã thanh toán cước trả hàng thành công với số tiền " + 
                            transaction.getAmount() + " VNĐ";
                        log.info("💰 Processing RETURN_SHIPPING payment notification for transaction: {}", transaction.getId());
                    } else {
                        // Default to full payment
                        notificationType = capstone_project.common.enums.NotificationTypeEnum.PAYMENT_FULL_SUCCESS;
                        title = "Thanh toán thành công";
                        description = "Đơn hàng " + order.getOrderCode() + " đã được thanh toán thành công với số tiền " + 
                            transaction.getAmount() + " VNĐ";
                        log.warn("⚠️ Unknown transaction type: {}, defaulting to PAYMENT_FULL_SUCCESS", transactionType);
                    }
                    
                    // Customer notification: Payment successful
                    log.info("🔍 Debug: Customer ID: {}, User ID: {}", customer.getId(), 
                        customer.getUser() != null ? customer.getUser().getId() : "NULL");
                    
                    if (customer.getUser() == null) {
                        log.error("❌ Customer {} has no associated user!", customer.getId());
                        return;
                    }
                    
                    CreateNotificationRequest customerNotification = CreateNotificationRequest.builder()
                        .userId(customer.getUser().getId())
                        .recipientRole("CUSTOMER")
                        .title(title)
                        .description(description)
                        .notificationType(notificationType)
                        .relatedOrderId(order.getId())
                        .relatedContractId(contract.getId())
                        .build();
                    
                    notificationService.createNotification(customerNotification);
                    
                    // Staff notification: Payment received - use correct notification type based on transaction type
                    if ("FULL_PAYMENT".equals(transactionType)) {
                        sendFullPaymentNotificationToStaff(order, transaction);
                        // Driver notification: NEW_ORDER_ASSIGNED when full payment received
                        sendFullPaymentNotificationToDriver(order);
                    } else {
                        sendPaymentNotificationToStaff(order, transaction, "Thanh toán mới nhận được", 
                            "Đơn hàng " + order.getOrderCode() + " đã thanh toán thành công");
                    }
                    
                    log.info("✅ {} notification created for transaction {}", notificationType, transaction.getId());
                    break;
                    
                case CANCELLED:
                    // Customer notification: Payment cancelled
                    if (customer.getUser() == null) {
                        log.error("❌ Customer {} has no associated user for cancellation notification!", customer.getId());
                        break;
                    }
                    CreateNotificationRequest cancelledNotification = CreateNotificationRequest.builder()
                        .userId(customer.getUser().getId())
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
                    break;
                    
                case REFUNDED:
                    // Customer notification: Payment refunded
                    if (customer.getUser() == null) {
                        log.error("❌ Customer {} has no associated user for refund notification!", customer.getId());
                        break;
                    }
                    CreateNotificationRequest refundNotification = CreateNotificationRequest.builder()
                        .userId(customer.getUser().getId())
                        .recipientRole("CUSTOMER")
                        .title("Đã hoàn tiền")
                        .description("Đã hoàn tiền cho đơn hàng " + order.getOrderCode())
                        .notificationType(capstone_project.common.enums.NotificationTypeEnum.ORDER_CANCELLED)
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
     * Helper method to send payment notifications to all staff users (for DEPOSIT)
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
                        .notificationType(capstone_project.common.enums.NotificationTypeEnum.STAFF_DEPOSIT_RECEIVED)
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
     * Helper method to send FULL_PAYMENT notifications to all staff users
     */
    private void sendFullPaymentNotificationToStaff(OrderEntity order, TransactionEntity transaction) {
        try {
            CustomerEntity customer = order.getSender();
            String customerName = "Khách hàng";
            if (customer != null) {
                customerName = customer.getRepresentativeName() != null ? 
                    customer.getRepresentativeName() : 
                    (customer.getUser() != null ? customer.getUser().getUsername() : "Khách hàng");
            }
            
            var staffUsers = userEntityService.getUserEntitiesByRoleRoleName("STAFF");
            for (var staff : staffUsers) {
                CreateNotificationRequest staffNotif = NotificationBuilder.buildStaffFullPayment(
                    staff.getId(),
                    order.getOrderCode(),
                    transaction.getAmount().doubleValue(),
                    customerName,
                    order.getId()
                );
                notificationService.createNotification(staffNotif);
            }
            log.info("✅ Created STAFF_FULL_PAYMENT notifications for {} staff users", staffUsers.size());
        } catch (Exception e) {
            log.error("❌ Failed to create STAFF_FULL_PAYMENT notification: {}", e.getMessage());
        }
    }
    
    /**
     * Helper method to send NEW_ORDER_ASSIGNED notification to driver when full payment received
     */
    private void sendFullPaymentNotificationToDriver(OrderEntity order) {
        try {
            List<capstone_project.entity.order.order.OrderDetailEntity> orderDetails = order.getOrderDetailEntities();
            
            if (orderDetails == null || orderDetails.isEmpty()) {
                log.warn("No order details found for order {}", order.getOrderCode());
                return;
            }
            
            capstone_project.entity.vehicle.VehicleAssignmentEntity assignment = 
                orderDetails.get(0).getVehicleAssignmentEntity();
            
            if (assignment == null || assignment.getDriver1() == null) {
                log.warn("No driver assigned for order {}", order.getOrderCode());
                return;
            }
            
            capstone_project.entity.user.driver.DriverEntity driver = assignment.getDriver1();
            
            // Get vehicle type description instead of name
            String vehicleTypeDescription = (assignment.getVehicleEntity() != null && 
                assignment.getVehicleEntity().getVehicleTypeEntity() != null &&
                assignment.getVehicleEntity().getVehicleTypeEntity().getDescription() != null) 
                ? assignment.getVehicleEntity().getVehicleTypeEntity().getDescription() : "Truck";
            
            // Get pickup location
            String pickupLocation = "Chưa xác định";
            if (order.getPickupAddress() != null) {
                capstone_project.entity.user.address.AddressEntity pickupAddr = order.getPickupAddress();
                pickupLocation = formatAddress(pickupAddr.getStreet(), pickupAddr.getWard(), pickupAddr.getProvince());
            }
            
            // Get delivery location
            String deliveryLocation = "Chưa xác định";
            if (order.getDeliveryAddress() != null) {
                capstone_project.entity.user.address.AddressEntity deliveryAddr = order.getDeliveryAddress();
                deliveryLocation = formatAddress(deliveryAddr.getStreet(), deliveryAddr.getWard(), deliveryAddr.getProvince());
            }
            
            // Get category description
            String categoryDescription = "Hàng hóa";
            if (order.getCategory() != null && order.getCategory().getDescription() != null) {
                categoryDescription = order.getCategory().getDescription();
            } else if (order.getCategory() != null && order.getCategory().getCategoryName() != null) {
                categoryDescription = order.getCategory().getCategoryName().name();
            }
            
            CreateNotificationRequest driverNotif = NotificationBuilder.buildNewOrderAssigned(
                driver.getUser().getId(),
                order.getOrderCode(),
                assignment.getTrackingCode() != null ? assignment.getTrackingCode() : "N/A",
                orderDetails,
                vehicleTypeDescription,
                (orderDetails.get(0).getEstimatedStartTime() != null)
                    ? orderDetails.get(0).getEstimatedStartTime() : null,
                pickupLocation,
                deliveryLocation,
                categoryDescription,
                order.getId(),
                assignment.getId()
            );
            
            notificationService.createNotification(driverNotif);
            log.info("✅ Created NEW_ORDER_ASSIGNED notification for driver: {} (Tracking: {})", 
                driver.getUser().getFullName(), assignment.getTrackingCode());
        } catch (Exception e) {
            log.error("❌ Failed to create NEW_ORDER_ASSIGNED notification: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Format address from street, ward, province components
     */
    private String formatAddress(String street, String ward, String province) {
        StringBuilder sb = new StringBuilder();
        if (street != null && !street.isEmpty()) {
            sb.append(street);
        }
        if (ward != null && !ward.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(ward);
        }
        if (province != null && !province.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(province);
        }
        return sb.length() > 0 ? sb.toString() : "Chưa xác định";
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
    
    /**
     * Helper method to send staff notifications
     * Creates a notification for each staff user from a template
     */
    private void sendStaffNotification(CreateNotificationRequest template) {
        try {
            var staffUsers = userEntityService.getUserEntitiesByRoleRoleName("STAFF");
            if (!staffUsers.isEmpty()) {
                for (var staff : staffUsers) {
                    // Create a new notification request for each staff user
                    CreateNotificationRequest staffNotification = CreateNotificationRequest.builder()
                        .userId(staff.getId())
                        .recipientRole("STAFF")
                        .title(template.getTitle())
                        .description(template.getDescription())
                        .notificationType(template.getNotificationType())
                        .relatedOrderId(template.getRelatedOrderId())
                        .relatedIssueId(template.getRelatedIssueId())
                        .relatedVehicleAssignmentId(template.getRelatedVehicleAssignmentId())
                        .relatedContractId(template.getRelatedContractId())
                        .metadata(template.getMetadata())
                        .build();
                    
                    notificationService.createNotification(staffNotification);
                }
                log.info("📧 Staff notifications sent: {} staff users notified, type: {}", 
                    staffUsers.size(), template.getNotificationType());
            }
        } catch (Exception e) {
            log.error("❌ Failed to send staff notification: {}", e.getMessage());
            // Don't throw - Notification failure shouldn't break business logic
        }
    }
}
