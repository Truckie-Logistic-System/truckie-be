package capstone_project.service.services.order.order.impl;

import capstone_project.common.enums.CategoryName;
import capstone_project.common.enums.CommonStatusEnum;
import capstone_project.common.enums.ContractStatusEnum;
import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.enums.OrderStatusEnum;
import capstone_project.common.exceptions.dto.BadRequestException;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.common.utils.BinPacker;
import capstone_project.common.utils.UserContextUtils;
import capstone_project.dtos.request.order.ContractRequest;
import capstone_project.dtos.request.order.CreateContractForCusRequest;
import capstone_project.dtos.request.order.contract.ContractFileUploadRequest;
import capstone_project.dtos.request.order.contract.GenerateContractPdfRequest;
import capstone_project.dtos.response.order.contract.*;
import capstone_project.entity.auth.UserEntity;
import capstone_project.entity.user.address.AddressEntity;
import capstone_project.entity.order.contract.ContractEntity;
import capstone_project.entity.order.contract.ContractRuleEntity;
import capstone_project.entity.order.order.CategoryPricingDetailEntity;
import capstone_project.entity.order.order.OrderDetailEntity;
import capstone_project.entity.order.order.OrderEntity;
import capstone_project.entity.order.order.OrderSizeEntity;
import capstone_project.entity.pricing.BasingPriceEntity;
import capstone_project.entity.pricing.DistanceRuleEntity;
import capstone_project.entity.pricing.SizeRuleEntity;
import capstone_project.repository.entityServices.auth.impl.UserEntityServiceImpl;
import capstone_project.repository.entityServices.order.contract.ContractEntityService;
import capstone_project.repository.entityServices.order.contract.ContractRuleEntityService;
import capstone_project.repository.entityServices.order.order.CategoryPricingDetailEntityService;
import capstone_project.repository.entityServices.order.order.OrderEntityService;
import capstone_project.repository.entityServices.order.order.OrderDetailEntityService;
import capstone_project.repository.entityServices.pricing.SizeRuleEntityService;
import capstone_project.repository.entityServices.pricing.DistanceRuleEntityService;
import capstone_project.repository.entityServices.pricing.BasingPriceEntityService;
import capstone_project.repository.repositories.order.order.OrderDetailRepository;
import capstone_project.repository.repositories.order.order.OrderRepository;
import capstone_project.repository.repositories.order.contract.ContractRuleRepository;
import capstone_project.repository.repositories.user.DriverRepository;
import capstone_project.repository.repositories.user.PenaltyHistoryRepository;
import capstone_project.repository.entityServices.order.transaction.TransactionEntityService;
import capstone_project.entity.order.transaction.TransactionEntity;
import capstone_project.service.services.vehicle.VehicleReservationService;
import capstone_project.repository.entityServices.vehicle.VehicleEntityService;
import capstone_project.repository.repositories.vehicle.VehicleAssignmentRepository;
import capstone_project.service.mapper.order.ContractMapper;
import capstone_project.service.services.cloudinary.CloudinaryService;
import capstone_project.service.services.order.order.ContractService;
import capstone_project.service.services.order.order.OrderStatusWebSocketService;
import capstone_project.service.services.setting.ContractSettingService;
import capstone_project.service.services.user.DistanceService;
import capstone_project.service.services.map.VietMapDistanceService;
import capstone_project.service.services.pricing.UnifiedPricingService;
import capstone_project.service.services.pricing.InsuranceCalculationService;
import capstone_project.service.services.notification.NotificationService;
import capstone_project.service.services.notification.NotificationBuilder;
import capstone_project.dtos.request.notification.CreateNotificationRequest;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ContractServiceImpl implements ContractService {

    private final ContractEntityService contractEntityService;
    private final ContractRuleEntityService contractRuleEntityService;
    private final SizeRuleEntityService sizeRuleEntityService;
    private final NotificationService notificationService;
    private final CategoryPricingDetailEntityService categoryPricingDetailEntityService;
    private final OrderEntityService orderEntityService;
    private final DistanceRuleEntityService distanceRuleEntityService;
    private final BasingPriceEntityService basingPriceEntityService;
    private final OrderDetailEntityService orderDetailEntityService;
    private final VehicleEntityService vehicleEntityService;
    private final DistanceService distanceService;
    private final VietMapDistanceService vietMapDistanceService;
    private final CloudinaryService cloudinaryService;
    private final UserContextUtils userContextUtils;
    private final OrderStatusWebSocketService orderStatusWebSocketService;
    private final UnifiedPricingService unifiedPricingService;
    private final InsuranceCalculationService insuranceCalculationService;
    private final ContractSettingService contractSettingService;
    private final capstone_project.repository.entityServices.auth.UserEntityService userEntityService; // For staff notifications
    private final VehicleReservationService vehicleReservationService;
    private final VehicleAssignmentRepository vehicleAssignmentRepository;
    private final TransactionEntityService transactionEntityService;
    
    private capstone_project.service.services.pdf.PdfGenerationService pdfGenerationService;

    private final ContractMapper contractMapper;

    private static final double EARTH_RADIUS_KM = 6371.0;
    private final UserEntityServiceImpl userEntityServiceImpl;

    // Manual constructor to break circular dependency
    public ContractServiceImpl(
            ContractEntityService contractEntityService,
            ContractRuleEntityService contractRuleEntityService,
            SizeRuleEntityService sizeRuleEntityService,
            NotificationService notificationService,
            CategoryPricingDetailEntityService categoryPricingDetailEntityService,
            OrderEntityService orderEntityService,
            DistanceRuleEntityService distanceRuleEntityService,
            BasingPriceEntityService basingPriceEntityService,
            OrderDetailEntityService orderDetailEntityService,
            VehicleEntityService vehicleEntityService,
            DistanceService distanceService,
            VietMapDistanceService vietMapDistanceService,
            CloudinaryService cloudinaryService,
            UserContextUtils userContextUtils,
            OrderStatusWebSocketService orderStatusWebSocketService,
            UnifiedPricingService unifiedPricingService,
            InsuranceCalculationService insuranceCalculationService,
            ContractSettingService contractSettingService,
            ContractMapper contractMapper,
            UserEntityServiceImpl userEntityServiceImpl,
            capstone_project.repository.entityServices.auth.UserEntityService userEntityService,
            VehicleReservationService vehicleReservationService,
            VehicleAssignmentRepository vehicleAssignmentRepository,
            TransactionEntityService transactionEntityService
    ) {
        this.contractEntityService = contractEntityService;
        this.contractRuleEntityService = contractRuleEntityService;
        this.sizeRuleEntityService = sizeRuleEntityService;
        this.notificationService = notificationService;
        this.categoryPricingDetailEntityService = categoryPricingDetailEntityService;
        this.orderEntityService = orderEntityService;
        this.distanceRuleEntityService = distanceRuleEntityService;
        this.basingPriceEntityService = basingPriceEntityService;
        this.orderDetailEntityService = orderDetailEntityService;
        this.vehicleEntityService = vehicleEntityService;
        this.distanceService = distanceService;
        this.vietMapDistanceService = vietMapDistanceService;
        this.cloudinaryService = cloudinaryService;
        this.userContextUtils = userContextUtils;
        this.orderStatusWebSocketService = orderStatusWebSocketService;
        this.unifiedPricingService = unifiedPricingService;
        this.insuranceCalculationService = insuranceCalculationService;
        this.contractSettingService = contractSettingService;
        this.contractMapper = contractMapper;
        this.userEntityServiceImpl = userEntityServiceImpl;
        this.userEntityService = userEntityService;
        this.vehicleReservationService = vehicleReservationService;
        this.vehicleAssignmentRepository = vehicleAssignmentRepository;
        this.transactionEntityService = transactionEntityService;
    }

    @Autowired
    @Lazy
    public void setPdfGenerationService(capstone_project.service.services.pdf.PdfGenerationService pdfGenerationService) {
        this.pdfGenerationService = pdfGenerationService;
    }

    @Override
    public List<ContractResponse> getAllContracts() {
        
        List<ContractEntity> contractEntities = contractEntityService.findAll();
        if (contractEntities.isEmpty()) {
            log.warn("No contracts found");
            throw new NotFoundException(
                    ErrorEnum.NOT_FOUND.getMessage(),
                    ErrorEnum.NOT_FOUND.getErrorCode()
            );
        }
        return contractEntities.stream()
                .map(contractMapper::toContractResponse)
                .toList();
    }

    @Override
    public ContractResponse getContractById(UUID id) {
        
        ContractEntity contractEntity = contractEntityService.findEntityById(id)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));
        return contractMapper.toContractResponse(contractEntity);
    }

    /**
     * Get effective contract value - prioritize adjustedValue if > 0, otherwise use totalValue
     * This ensures notifications show the correct payment amounts
     */
    private double getEffectiveContractValue(ContractEntity contract) {
        if (contract.getAdjustedValue() != null && contract.getAdjustedValue().doubleValue() > 0) {
            return contract.getAdjustedValue().doubleValue();
        }
        return contract.getTotalValue() != null ? contract.getTotalValue().doubleValue() : 0.0;
    }

    @Override
    @Transactional
    public ContractResponse createContract(ContractRequest contractRequest) {

        if (contractRequest == null) {
            log.error("[createContract] Request is null");
            throw new BadRequestException("Contract request must not be null",
                    ErrorEnum.INVALID_REQUEST.getErrorCode());
        }

        if (contractRequest.orderId() == null) {
            log.error("[createContract] Order ID is null in contract request");
            throw new BadRequestException("Order ID must not be null",
                    ErrorEnum.NULL.getErrorCode());
        }

        UUID orderUuid;
        try {
            orderUuid = UUID.fromString(contractRequest.orderId());
        } catch (IllegalArgumentException e) {
            log.error("[createContract] Invalid orderId format: {}", contractRequest.orderId());
            throw e;
        }

        if (contractEntityService.getContractByOrderId(orderUuid).isPresent()) {
            log.error("[createContract] Contract already exists for order ID: {}", orderUuid);
            throw new BadRequestException(ErrorEnum.ALREADY_EXISTED.getMessage(),
                    ErrorEnum.ALREADY_EXISTED.getErrorCode());
        }

        OrderEntity order = orderEntityService.findEntityById(orderUuid)
                .orElseThrow(() -> {
                    log.error("[createContract] Order not found: {}", orderUuid);
                    return new NotFoundException("Order not found", ErrorEnum.NOT_FOUND.getErrorCode());
                });

        ContractEntity contractEntity = contractMapper.mapRequestToEntity(contractRequest);
        contractEntity.setStatus(ContractStatusEnum.CONTRACT_DRAFT.name());
        contractEntity.setOrderEntity(order);

        ContractEntity savedContract = contractEntityService.save(contractEntity);
        
        // Create notification for contract ready
        try {
            // Get order details for package information
            List<OrderDetailEntity> orderDetails = orderDetailEntityService.findOrderDetailEntitiesByOrderEntityId(order.getId());
            
            // Calculate deposit amount using contract settings
            double totalAmount = getEffectiveContractValue(savedContract);
            double depositAmount = 0.0;
            
            try {
                var contractSetting = contractSettingService.getLatestContractSetting();
                if (contractSetting != null && contractSetting.depositPercent() != null) {
                    // Use effective contract value (adjustedValue prioritized over totalValue)
                    double baseValue = getEffectiveContractValue(savedContract);
                    depositAmount = baseValue * contractSetting.depositPercent().doubleValue() / 100.0;
                }
            } catch (Exception e) {
                log.warn("Could not get contract setting for deposit calculation, using 30% default: {}", e.getMessage());
                // Fallback to 30% if contract setting fails
                double baseValue = getEffectiveContractValue(savedContract);
                depositAmount = baseValue * 0.1;
            }
            
            String contractCode = savedContract.getContractName() != null ? savedContract.getContractName() : "HĐ-" + order.getOrderCode();
            
            CreateNotificationRequest notificationRequest = NotificationBuilder.buildContractReady(
                order.getSender().getUser().getId(),
                order.getOrderCode(),
                contractCode,
                depositAmount,
                totalAmount,
                savedContract.getSigningDeadline(),
                savedContract.getDepositPaymentDeadline(),
                orderDetails,
                order.getId(),
                savedContract.getId()
            );
            
            notificationService.createNotification(notificationRequest);
            log.info("✅ Created CONTRACT_READY notification for order: {} with deposit: {}", order.getOrderCode(), depositAmount);
        } catch (Exception e) {
            log.error("❌ Failed to create CONTRACT_READY notification: {}", e.getMessage());
        }

        return contractMapper.toContractResponse(savedContract);
    }

    @Override
    @Transactional
    public ContractResponse createBothContractAndContractRule(ContractRequest contractRequest) {

        if (contractRequest == null) {
            log.error("[createContract] Request is null");
            throw new BadRequestException("Contract request must not be null",
                    ErrorEnum.INVALID_REQUEST.getErrorCode());
        }

        if (contractRequest.orderId() == null) {
            log.error("[createContract] Order ID is null in contract request");
            throw new BadRequestException("Order ID must not be null",
                    ErrorEnum.NULL.getErrorCode());
        }

        UUID orderUuid;
        try {
            orderUuid = UUID.fromString(contractRequest.orderId());
        } catch (IllegalArgumentException e) {
            log.error("[createBoth] Invalid orderId format: {}", contractRequest.orderId());
            throw e;
        }

        if (contractEntityService.getContractByOrderId(orderUuid).isPresent()) {
            log.error("[createBoth] Contract already exists for orderId={}", orderUuid);
            throw new BadRequestException(ErrorEnum.ALREADY_EXISTED.getMessage(),
                    ErrorEnum.ALREADY_EXISTED.getErrorCode());
//            deleteContractByOrderId(orderUuid);
        }

        OrderEntity order = orderEntityService.findEntityById(orderUuid)
                .orElseThrow(() -> {
                    log.error("[createBoth] Order not found: {}", orderUuid);
                    return new NotFoundException("Order not found", ErrorEnum.NOT_FOUND.getErrorCode());
                });

        BigDecimal distanceKm = distanceService.getDistanceInKilometers(order.getId());

        ContractEntity contractEntity = contractMapper.mapRequestToEntity(contractRequest);
        contractEntity.setStatus(ContractStatusEnum.CONTRACT_DRAFT.name());
        contractEntity.setOrderEntity(order);
        
        // Set contract deadlines
        setContractDeadlines(contractEntity, order);

        ContractEntity savedContract = contractEntityService.save(contractEntity);

        List<ContractRuleAssignResponse> assignments = assignVehiclesWithAvailability(orderUuid);

        Map<UUID, Integer> vehicleCountMap = assignments.stream()
                .collect(Collectors.groupingBy(ContractRuleAssignResponse::getSizeRuleId, Collectors.summingInt(a -> 1)));

        for (Map.Entry<UUID, Integer> entry : vehicleCountMap.entrySet()) {
            UUID sizeRuleId = entry.getKey();
            Integer count = entry.getValue();

            SizeRuleEntity sizeRule = sizeRuleEntityService.findEntityById(sizeRuleId)
                    .orElseThrow(() -> {
                        log.error("[createBoth] Vehicle rule not found: {}", sizeRuleId);
                        return new NotFoundException("Vehicle rule not found", ErrorEnum.NOT_FOUND.getErrorCode());
                    });

            ContractRuleEntity contractRule = ContractRuleEntity.builder()
                    .contractEntity(savedContract)
                    .sizeRuleEntity(sizeRule)
                    .numOfVehicles(count)
                    .status(CommonStatusEnum.ACTIVE.name())
                    .build();

            // 🔑 Lấy các orderDetails từ assignments
            List<OrderDetailForPackingResponse> detailResponses = assignments.stream()
                    .filter(a -> a.getSizeRuleId().equals(sizeRuleId))
                    .flatMap(a -> a.getAssignedDetails().stream())
                    .toList();

//            List<OrderDetailForPackingResponse> detailIds = assignments.stream()

            if (!detailResponses.isEmpty()) {
                List<UUID> detailIds = detailResponses.stream()
                        .map(r -> UUID.fromString(r.id()))
                        .toList();

                List<OrderDetailEntity> orderDetailEntities = orderDetailEntityService.findAllByIds(detailIds);
                contractRule.getOrderDetails().addAll(orderDetailEntities);
            }

            contractRuleEntityService.save(contractRule);
        }
        
        OrderStatusEnum previousStatus = OrderStatusEnum.valueOf(order.getStatus());
        order.setStatus(OrderStatusEnum.PROCESSING.name());
        orderEntityService.save(order);
        
        // Send WebSocket notification for status change
        try {
            orderStatusWebSocketService.sendOrderStatusChange(
                order.getId(),
                order.getOrderCode(),
                previousStatus,
                OrderStatusEnum.PROCESSING
            );
        } catch (Exception e) {
            log.error("Failed to send WebSocket notification for order status change: {}", e.getMessage());
        }

        PriceCalculationResponse totalPriceResponse = calculateTotalPrice(savedContract, distanceKm, vehicleCountMap);

        // Use grandTotal which includes insurance fee (if applicable)
        BigDecimal grandTotal = totalPriceResponse.getGrandTotal();

        UserEntity currentStaff = userContextUtils.getCurrentUser();

        savedContract.setTotalValue(grandTotal);
        savedContract.setStaff(currentStaff);
        
        ContractEntity updatedContract = contractEntityService.save(savedContract);

        // Create notifications when customer agrees to vehicle proposal via /contracts/both endpoint
        try {
            // Get order details for package information
            List<OrderDetailEntity> orderDetails = order.getOrderDetailEntities() != null ? 
                order.getOrderDetailEntities() : new ArrayList<>();
            
            // Notification 1: Customer - ORDER_PROCESSING (Email: NO) with full package details
            CreateNotificationRequest customerNotification = NotificationBuilder.buildOrderProcessing(
                order.getSender().getUser().getId(),
                order.getOrderCode(),
                orderDetails,
                order.getId()
            );
            notificationService.createNotification(customerNotification);
            log.info("✅ Created ORDER_PROCESSING notification for customer in order: {}", order.getOrderCode());
            
            // Notification 2: All Staff - STAFF_ORDER_PROCESSING with full package details
            String customerName = order.getSender().getRepresentativeName() != null ?
                order.getSender().getRepresentativeName() : order.getSender().getUser().getUsername();
            String customerPhone = order.getSender().getRepresentativePhone() != null ?
                order.getSender().getRepresentativePhone() : "N/A";
            
            var staffUsers = userEntityService.getUserEntitiesByRoleRoleName("STAFF");
            for (var staff : staffUsers) {
                CreateNotificationRequest staffNotification = NotificationBuilder.buildStaffOrderProcessing(
                    staff.getId(),
                    order.getOrderCode(),
                    customerName,
                    customerPhone,
                    orderDetails,
                    order.getId()
                );
                notificationService.createNotification(staffNotification);
            }
            log.info("✅ Created STAFF_ORDER_PROCESSING notifications for {} staff users", staffUsers.size());
            
        } catch (Exception e) {
            log.error("❌ Failed to create contract notifications: {}", e.getMessage());
            // Don't throw - notification failure shouldn't break contract creation
        }

        return contractMapper.toContractResponse(updatedContract);
    }

    @Override
    @Transactional
    public ContractResponse createBothContractAndContractRuleForCus(CreateContractForCusRequest contractRequest) {

        if (contractRequest == null) {
            log.error("[createContract] Request is null");
            throw new BadRequestException("Contract request must not be null",
                    ErrorEnum.INVALID_REQUEST.getErrorCode());
        }

        if (contractRequest.orderId() == null) {
            log.error("[createContract] Order ID is null in contract request");
            throw new BadRequestException("Order ID must not be null",
                    ErrorEnum.NULL.getErrorCode());
        }

        UUID orderUuid;
        try {
            orderUuid = UUID.fromString(contractRequest.orderId());
        } catch (IllegalArgumentException e) {
            log.error("[createBoth] Invalid orderId format: {}", contractRequest.orderId());
            throw e;
        }

        if (contractEntityService.getContractByOrderId(orderUuid).isPresent()) {
            log.error("[createBoth] Contract already exists for orderId={}", orderUuid);
            throw new BadRequestException(ErrorEnum.ALREADY_EXISTED.getMessage(),
                    ErrorEnum.ALREADY_EXISTED.getErrorCode());
//            deleteContractByOrderId(orderUuid);
        }

        OrderEntity order = orderEntityService.findEntityById(orderUuid)
                .orElseThrow(() -> {
                    log.error("[createBoth] Order not found: {}", orderUuid);
                    return new NotFoundException("Order not found", ErrorEnum.NOT_FOUND.getErrorCode());
                });

        BigDecimal distanceKm = distanceService.getDistanceInKilometers(order.getId());

        ContractEntity contractEntity = contractMapper.mapRequestForCusToEntity(contractRequest);
        contractEntity.setStatus(ContractStatusEnum.CONTRACT_DRAFT.name());
        contractEntity.setOrderEntity(order);
        
        // Set contract deadlines
        setContractDeadlines(contractEntity, order);

        ContractEntity savedContract = contractEntityService.save(contractEntity);

        // Create CONTRACT_READY notification for customer with email
        try {
            String contractCode = savedContract.getContractName() != null ? savedContract.getContractName() : "HĐ-" + order.getOrderCode();
            double depositAmount = 0.0; // Will be set by contract rules later
            double totalAmount = getEffectiveContractValue(savedContract);
            
            log.info("🔍 Creating CONTRACT_READY notification for order: {} with contract: {}", 
                order.getOrderCode(), contractCode);
            
            CreateNotificationRequest notificationRequest = NotificationBuilder.buildContractReady(
                order.getSender().getUser().getId(),
                order.getOrderCode(),
                contractCode,
                depositAmount,
                totalAmount,
                savedContract.getSigningDeadline(),
                savedContract.getDepositPaymentDeadline(),
                order.getId(),
                savedContract.getId()
            );
            
            notificationService.createNotification(notificationRequest);
            log.info("✅ Created CONTRACT_READY notification for order: {}", order.getOrderCode());
        } catch (Exception e) {
            log.error("❌ Failed to create CONTRACT_READY notification: {}", e.getMessage());
            // Don't throw - notification failure shouldn't break contract creation
        }

        List<ContractRuleAssignResponse> assignments = assignVehiclesWithAvailability(orderUuid);

        Map<UUID, Integer> vehicleCountMap = assignments.stream()
                .collect(Collectors.groupingBy(ContractRuleAssignResponse::getSizeRuleId, Collectors.summingInt(a -> 1)));

        for (Map.Entry<UUID, Integer> entry : vehicleCountMap.entrySet()) {
            UUID sizeRuleId = entry.getKey();
            Integer count = entry.getValue();

            SizeRuleEntity sizeRule = sizeRuleEntityService.findEntityById(sizeRuleId)
                    .orElseThrow(() -> {
                        log.error("[createBoth] Vehicle rule not found: {}", sizeRuleId);
                        return new NotFoundException("Vehicle rule not found", ErrorEnum.NOT_FOUND.getErrorCode());
                    });

            ContractRuleEntity contractRule = ContractRuleEntity.builder()
                    .contractEntity(savedContract)
                    .sizeRuleEntity(sizeRule)
                    .numOfVehicles(count)
                    .status(CommonStatusEnum.ACTIVE.name())
                    .build();

            // 🔑 Lấy các orderDetails từ assignments
            List<OrderDetailForPackingResponse> detailResponses = assignments.stream()
                    .filter(a -> a.getSizeRuleId().equals(sizeRuleId))
                    .flatMap(a -> a.getAssignedDetails().stream())
                    .toList();

//            List<OrderDetailForPackingResponse> detailIds = assignments.stream()

            if (!detailResponses.isEmpty()) {
                List<UUID> detailIds = detailResponses.stream()
                        .map(r -> UUID.fromString(r.id()))
                        .toList();

                List<OrderDetailEntity> orderDetailEntities = orderDetailEntityService.findAllByIds(detailIds);
                contractRule.getOrderDetails().addAll(orderDetailEntities);
            }

            contractRuleEntityService.save(contractRule);
        }
        
        OrderStatusEnum previousStatus = OrderStatusEnum.valueOf(order.getStatus());
        order.setStatus(OrderStatusEnum.PROCESSING.name());
        orderEntityService.save(order);
        
        // Send WebSocket notification for status change
        try {
            orderStatusWebSocketService.sendOrderStatusChange(
                order.getId(),
                order.getOrderCode(),
                previousStatus,
                OrderStatusEnum.PROCESSING
            );
            
        } catch (Exception e) {
            log.error("Failed to send WebSocket notification for order status change: {}", e.getMessage());
            // Don't throw - WebSocket failure shouldn't break business logic
        }

        PriceCalculationResponse totalPriceResponse = calculateTotalPrice(savedContract, distanceKm, vehicleCountMap);

        // Use grandTotal which includes insurance fee (if applicable)
        BigDecimal grandTotal = totalPriceResponse.getGrandTotal();

        UserEntity currentStaff = userContextUtils.getCurrentUser();

        savedContract.setTotalValue(grandTotal);
        savedContract.setStaff(currentStaff);
        
        ContractEntity updatedContract = contractEntityService.save(savedContract);

        // Auto-reservation: Create vehicle reservations when customer accepts proposal
        if (contractRequest.tripDate() != null && contractRequest.vehicleCount() != null && contractRequest.vehicleCount() > 0) {
            try {
                LocalDate reservationDate = contractRequest.tripDate().toLocalDate();
                int vehiclesToReserve = contractRequest.vehicleCount();
                
                log.info("🚗 [createBothContractAndContractRuleForCus] Creating auto-reservation for order {} on {} for {} vehicles", 
                        orderUuid, reservationDate, vehiclesToReserve);
                
                // Get realistic assignments to find available vehicles
                List<ContractRuleAssignResponse> realisticAssignments = assignVehiclesWithAvailability(orderUuid);
                
                // Collect all available vehicle types from assignments
                Set<UUID> availableVehicleTypeIds = realisticAssignments.stream()
                        .map(ContractRuleAssignResponse::getSizeRuleId)
                        .collect(Collectors.toSet());
                
                // Get available vehicles for each type, sorted by usage (least used first)
                List<UUID> selectedVehicleIds = new ArrayList<>();
                
                for (UUID sizeRuleId : availableVehicleTypeIds) {
                    if (selectedVehicleIds.size() >= vehiclesToReserve) break;
                    
                    SizeRuleEntity sizeRule = sizeRuleEntityService.findEntityById(sizeRuleId)
                            .orElseThrow(() -> new NotFoundException("Size rule not found", ErrorEnum.NOT_FOUND.getErrorCode()));
                    
                    // Get available vehicles for this type, sorted by usage (least used first)
                    List<UUID> availableVehicles = vehicleEntityService.getVehicleEntitiesByVehicleTypeEntityAndStatus(
                            sizeRule.getVehicleTypeEntity(), CommonStatusEnum.ACTIVE.name())
                            .stream()
                            .filter(v -> vehicleReservationService.isVehicleAvailable(v.getId(), reservationDate, orderUuid))
                            .sorted(Comparator.comparing(v -> getVehicleUsageCount(v.getId()))) // Least used first
                            .map(v -> v.getId())
                            .limit(vehiclesToReserve - selectedVehicleIds.size())
                            .toList();
                    
                    selectedVehicleIds.addAll(availableVehicles);
                }
                
                // Create reservations for selected vehicles
                if (!selectedVehicleIds.isEmpty()) {
                    vehicleReservationService.createReservationsForOrder(
                            orderUuid,
                            selectedVehicleIds,
                            reservationDate,
                            "Auto-reservation when customer accepted vehicle proposal"
                    );
                    
                    log.info("✅ [createBothContractAndContractRuleForCus] Created {} vehicle reservations for order {}", 
                            selectedVehicleIds.size(), orderUuid);
                } else {
                    log.warn("⚠️ [createBothContractAndContractRuleForCus] No available vehicles to reserve for order {}", orderUuid);
                }
                
            } catch (Exception e) {
                log.error("❌ [createBothContractAndContractRuleForCus] Failed to create auto-reservation for order {}: {}", 
                        orderUuid, e.getMessage());
                // Don't throw - reservation failure shouldn't break contract creation
            }
        }

        return contractMapper.toContractResponse(updatedContract);
    }

    /**
     * Get vehicle usage count for load balancing (least used first)
     */
    private long getVehicleUsageCount(UUID vehicleId) {
        try {
            return vehicleAssignmentRepository.countByVehicleEntityId(vehicleId);
        } catch (Exception e) {
            log.warn("Could not get usage count for vehicle {}: {}", vehicleId, e.getMessage());
            return 0; // Default to 0 if query fails
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BothOptimalAndRealisticAssignVehiclesResponse getBothOptimalAndRealisticAssignVehiclesResponse(UUID orderId) {
        log.info("[getBothOptimalAndRealisticAssignVehiclesResponse] START for orderId={}", orderId);
        
        List<ContractRuleAssignResponse> optimal = null;
        List<ContractRuleAssignResponse> realistic = null;

        try {
            log.info("[getBothOptimalAndRealisticAssignVehiclesResponse] Calling assignVehiclesOptimal...");
            optimal = assignVehiclesOptimal(orderId);
            log.info("[getBothOptimalAndRealisticAssignVehiclesResponse] assignVehiclesOptimal completed, result size: {}", 
                    optimal != null ? optimal.size() : "null");
        } catch (Exception e) {
            log.error("[getBothOptimalAndRealisticAssignVehiclesResponse] Optimal assignment failed for orderId={}", orderId, e);
            throw e; // Re-throw to return proper error response
        }

        try {
            log.info("[getBothOptimalAndRealisticAssignVehiclesResponse] Calling assignVehiclesWithAvailability...");
            realistic = assignVehiclesWithAvailability(orderId);
            log.info("[getBothOptimalAndRealisticAssignVehiclesResponse] assignVehiclesWithAvailability completed, result size: {}", 
                    realistic != null ? realistic.size() : "null");
        } catch (Exception e) {
            log.error("[getBothOptimalAndRealisticAssignVehiclesResponse] Realistic assignment failed for orderId={}", orderId, e);
            throw e; // Re-throw to return proper error response
        }

        if (optimal == null && realistic == null) {
            log.warn("[getBothOptimalAndRealisticAssignVehiclesResponse] Both optimal and realistic are null for orderId={}", orderId);
            return null;
        }

        log.info("[getBothOptimalAndRealisticAssignVehiclesResponse] SUCCESS for orderId={}", orderId);
        return new BothOptimalAndRealisticAssignVehiclesResponse(optimal, realistic);
    }

    @Override
    public ContractResponse updateContract(UUID id, ContractRequest contractRequest) {
        return null;
    }

    @Override
    @Transactional
    public void deleteContractByOrderId(UUID orderId) {

        if (orderId == null) {
            log.error("[deleteContractByOrderId] Order ID is null");
            throw new BadRequestException("Order ID must not be null",
                    ErrorEnum.NULL.getErrorCode());
        }

        ContractEntity contractEntity = contractEntityService.getContractByOrderId(orderId)
                .orElseThrow(() -> new NotFoundException(
                        "Contract not found for order ID: " + orderId,
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        List<ContractRuleEntity> ruleEntity = contractRuleEntityService.findContractRuleEntityByContractEntityId(contractEntity.getId());
        if (!ruleEntity.isEmpty()) {
            ruleEntity.forEach(rule -> rule.getOrderDetails().clear());
            contractRuleEntityService.saveAll(ruleEntity);
            contractRuleEntityService.deleteByContractEntityId(contractEntity.getId());
        }
        contractEntityService.deleteContractByOrderId(orderId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContractRuleAssignResponse> assignVehiclesWithAvailability(UUID orderId) {
        log.info("[assignVehiclesWithAvailability] START for orderId={}", orderId);
        List<ContractRuleAssignResponse> optimal = assignVehiclesOptimal(orderId);

        OrderEntity orderEntity = orderEntityService.findEntityById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found", ErrorEnum.NOT_FOUND.getErrorCode()));

        // Null-safe sorting to avoid NPE
        List<SizeRuleEntity> sortedSizeRules = sizeRuleEntityService
                .findAllByCategoryId(orderEntity.getCategory().getId())
                .stream()
                .filter(rule -> CommonStatusEnum.ACTIVE.name().equals(rule.getStatus()))
                .filter(rule -> rule.getMaxWeight() != null) // Filter out rules with null maxWeight
                .sorted(Comparator.comparing(
                        (SizeRuleEntity r) -> r.getMaxWeight() != null ? r.getMaxWeight() : BigDecimal.ZERO)
                        .thenComparing(r -> r.getMaxLength() != null ? r.getMaxLength() : BigDecimal.ZERO)
                        .thenComparing(r -> r.getMaxWidth() != null ? r.getMaxWidth() : BigDecimal.ZERO)
                        .thenComparing(r -> r.getMaxHeight() != null ? r.getMaxHeight() : BigDecimal.ZERO))
                .toList();

        // map ruleId -> số lượng xe khả dụng
        Map<UUID, Integer> availableVehicles = new HashMap<>();
        for (SizeRuleEntity rule : sortedSizeRules) {
            int count = vehicleEntityService
                    .getVehicleEntitiesByVehicleTypeEntityAndStatus(
                            rule.getVehicleTypeEntity(),
                            CommonStatusEnum.ACTIVE.name()
                    ).size();
            availableVehicles.put(rule.getId(), count);
        }

        // map ruleId -> số lượng xe đã sử dụng
        Map<UUID, Integer> usedVehicles = new HashMap<>();
        List<ContractRuleAssignResponse> realisticAssignments = new ArrayList<>();

        for (ContractRuleAssignResponse assignment : optimal) {
            UUID ruleId = assignment.getSizeRuleId();
            int used = usedVehicles.getOrDefault(ruleId, 0);
            int available = availableVehicles.getOrDefault(ruleId, 0);

            if (used < available) {
                // còn xe → gán
                realisticAssignments.add(assignment);
                usedVehicles.put(ruleId, used + 1);
            } else {
                // hết xe → upgrade
                SizeRuleEntity currentRule = sortedSizeRules.get(assignment.getVehicleIndex());
                SizeRuleEntity upgradedRule = tryUpgradeUntilAvailable(
                        assignment, currentRule, sortedSizeRules, availableVehicles, usedVehicles
                );

                if (upgradedRule != null) {
                    assignment.setSizeRuleId(upgradedRule.getId());
                    assignment.setSizeRuleName(upgradedRule.getSizeRuleName());
                    assignment.setVehicleIndex(sortedSizeRules.indexOf(upgradedRule));
                    realisticAssignments.add(assignment);

                    usedVehicles.put(upgradedRule.getId(),
                            usedVehicles.getOrDefault(upgradedRule.getId(), 0) + 1);
                } else {
                    log.error("Không có xe nào đủ khả dụng để chở cho order {}", orderId);
                    throw new BadRequestException(
                            ErrorEnum.NO_VEHICLE_AVAILABLE.getMessage(),
                            ErrorEnum.NO_VEHICLE_AVAILABLE.getErrorCode()
                    );
                }
            }
        }

        return realisticAssignments;
    }

    private SizeRuleEntity tryUpgradeUntilAvailable(ContractRuleAssignResponse assignment,
                                                    SizeRuleEntity currentRule,
                                                    List<SizeRuleEntity> sortedRules,
                                                    Map<UUID, Integer> availableVehicles,
                                                    Map<UUID, Integer> usedVehicles) {

        int currentIdx = sortedRules.indexOf(currentRule);

        for (int nextIdx = currentIdx + 1; nextIdx < sortedRules.size(); nextIdx++) {
            SizeRuleEntity nextRule = sortedRules.get(nextIdx);
            int used = usedVehicles.getOrDefault(nextRule.getId(), 0);
            int available = availableVehicles.getOrDefault(nextRule.getId(), 0);

            if (used < available) {
                
                return nextRule;
            }
        }
        return null;
    }

//    public List<ContractRuleAssignResponse> assignVehiclesOptimal(UUID orderId) {
//        final long t0 = System.nanoTime();
//        
//
//        List<OrderDetailEntity> details = orderDetailEntityService.findOrderDetailEntitiesByOrderEntityId(orderId);
//        if (details.isEmpty()) {
//            log.error("[assignVehicles] Order details not found for orderId={}", orderId);
//            throw new NotFoundException("No order details found for this order", ErrorEnum.NOT_FOUND.getErrorCode());
//        }
//
//        OrderEntity orderEntity = orderEntityService.findEntityById(orderId)
//                .orElseThrow(() -> {
//                    log.error("[assignVehicles] Order not found: {}", orderId);
//                    return new NotFoundException("Order not found", ErrorEnum.NOT_FOUND.getErrorCode());
//                });
//
//        if (orderEntity.getCategory() == null) {
//            log.error("[assignVehicles] Order category is null for orderId={}", orderId);
//            throw new BadRequestException("Order category is required", ErrorEnum.INVALID.getErrorCode());
//        }
//
//        // Lấy rule theo category, sort theo kluong + kích thước
//        List<sizeRuleEntity> sortedsizeRules = sizeRuleEntityService
//                .findAllByCategoryId(orderEntity.getCategory().getId())
//                .stream()
//                .filter(rule -> CommonStatusEnum.ACTIVE.name().equals(rule.getStatus()))
//                .sorted(Comparator.comparing(sizeRuleEntity::getMaxWeight)
//                        .thenComparing(sizeRuleEntity::getMaxLength)
//                        .thenComparing(sizeRuleEntity::getMaxWidth)
//                        .thenComparing(sizeRuleEntity::getMaxHeight))
//                .toList();
//
//        if (sortedsizeRules.isEmpty()) {
//            log.error("[assignVehicles] No vehicle rules found for categoryId={}", orderEntity.getCategory().getId());
//            throw new NotFoundException("No vehicle rules found for this category", ErrorEnum.NOT_FOUND.getErrorCode());
//        }
//
//        // Map ruleId -> rule, ruleId -> index
//        Map<UUID, sizeRuleEntity> ruleById = sortedsizeRules.stream()
//                .collect(Collectors.toMap(sizeRuleEntity::getId, Function.identity()));
//        Map<UUID, Integer> ruleIndexById = new HashMap<>();
//        for (int i = 0; i < sortedsizeRules.size(); i++) {
//            ruleIndexById.put(sortedsizeRules.get(i).getId(), i);
//        }
//
//        // Sort details (FFD: kiện   to trước)
//        details.sort((a, b) -> {
//            int cmp = b.getWeight().compareTo(a.getWeight());
//            if (cmp == 0) cmp = b.getOrderSizeEntity().getMaxLength().compareTo(a.getOrderSizeEntity().getMaxLength());
//            if (cmp == 0) cmp = b.getOrderSizeEntity().getMaxWidth().compareTo(a.getOrderSizeEntity().getMaxWidth());
//            if (cmp == 0) cmp = b.getOrderSizeEntity().getMaxHeight().compareTo(a.getOrderSizeEntity().getMaxHeight());
//            return cmp;
//        });
//
//        List<ContractRuleAssignResponse> assignments = new ArrayList<>();
//        int processed = 0;
//
//        // Gán kiện   vào xe
//        for (OrderDetailEntity detail : details) {
//            processed++;
//            if (detail.getOrderSizeEntity() == null) {
//                log.warn("[assignVehicles] Detail id={} missing orderSize", detail.getId());
//                throw new BadRequestException("Order detail missing size: " + detail.getId(), ErrorEnum.INVALID.getErrorCode());
//            }
//
//            
//
//            boolean assigned = false;
//
//            // thử gán vào xe đã mở
//            for (ContractRuleAssignResponse assignment : assignments) {
//                sizeRuleEntity currentRule = ruleById.get(assignment.getsizeRuleId());
//                if (currentRule == null) {
//                    log.error("[assignVehicles] Missing rule for id={}", assignment.getsizeRuleId());
//                    continue;
//                }
//
//                if (canFit(detail, currentRule, assignment)) {
//                    assignment.setCurrentLoad(assignment.getCurrentLoad().add(detail.getWeight()));
//                    assignment.getAssignedDetails().add(toPackingResponse(detail));
//                    
//                    assigned = true;
//                    break;
//                }
//
//                // thử upgrade
//                sizeRuleEntity upgradedRule = tryUpgrade(detail, assignment, sortedsizeRules);
//                if (upgradedRule != null) {
//                    assignment.setsizeRuleId(upgradedRule.getId());
//                    assignment.setsizeRuleName(upgradedRule.getsizeRuleName());
//                    assignment.setCurrentLoad(calculateTotalWeight(assignment, detail));
//                    assignment.getAssignedDetails().add(toPackingResponse(detail));
//                    
//                    assigned = true;
//                    break;
//                }
//            }
//
//            // nếu chưa gán được -> mở xe mới
//            if (!assigned) {
//                for (sizeRuleEntity rule : sortedsizeRules) {
//                    if (canFit(detail, rule)) {
//                        ContractRuleAssignResponse newAssignment = new ContractRuleAssignResponse(
//                                ruleIndexById.get(rule.getId()),
//                                rule.getId(),
//                                rule.getsizeRuleName(),
//                                detail.getWeight(),
//                                new ArrayList<>(List.of(toPackingResponse(detail)))
//                        );
//                        assignments.add(newAssignment);
//                        
//                        assigned = true;
//                        break;
//                    }
//                }
//            }
//
//            if (!assigned) {
//                log.error("[assignVehicles] No vehicle can carry detail {}", detail.getId());
//                throw new RuntimeException("Không có loại xe nào chở được kiện   " + detail.getId());
//            }
//        }
//
//        
//        long elapsedMs = (System.nanoTime() - t0) / 1_000_000;
//        
//        return assignments;
//    }

    @Override
    @Transactional(readOnly = true)
    public List<ContractRuleAssignResponse> assignVehiclesOptimal(UUID orderId) {
        final long t0 = System.nanoTime();
        log.info("[assignVehiclesOptimal] START for orderId={}", orderId);

        OrderEntity orderEntity = orderEntityService.findEntityById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found", ErrorEnum.NOT_FOUND.getErrorCode()));

        List<OrderDetailEntity> details = orderDetailEntityService.findOrderDetailEntitiesByOrderEntityId(orderId);
        if (details.isEmpty()) {
            throw new NotFoundException("No order details found for this order", ErrorEnum.NOT_FOUND.getErrorCode());
        }
        log.info("[assignVehiclesOptimal] Found {} order details", details.size());

        if (orderEntity.getCategory() == null) {
            log.error("[assignVehiclesOptimal] Order category is null for orderId={}", orderId);
            throw new BadRequestException("Order category is required", ErrorEnum.INVALID.getErrorCode());
        }

        UUID categoryId = orderEntity.getCategory().getId();
        String categoryName = orderEntity.getCategory().getCategoryName().name();
        log.info("[assignVehiclesOptimal] Order category: id={}, name='{}'", categoryId, categoryName);

        // Fetch and filter size rules with null-safe sorting
        List<SizeRuleEntity> sortedsizeRules = sizeRuleEntityService
                .findAllByCategoryId(categoryId)
                .stream()
                .filter(rule -> CommonStatusEnum.ACTIVE.name().equals(rule.getStatus()))
                .filter(rule -> rule.getMaxWeight() != null) // Filter out rules with null maxWeight
                .sorted(Comparator.comparing(
                        (SizeRuleEntity r) -> r.getMaxWeight() != null ? r.getMaxWeight() : BigDecimal.ZERO)
                        .thenComparing(r -> r.getMaxLength() != null ? r.getMaxLength() : BigDecimal.ZERO)
                        .thenComparing(r -> r.getMaxWidth() != null ? r.getMaxWidth() : BigDecimal.ZERO)
                        .thenComparing(r -> r.getMaxHeight() != null ? r.getMaxHeight() : BigDecimal.ZERO))
                .toList();

        log.info("[assignVehiclesOptimal] Found {} ACTIVE size rules for categoryId={}", sortedsizeRules.size(), categoryId);
        
        // Log each size rule for debugging
        for (SizeRuleEntity rule : sortedsizeRules) {
            log.info("[assignVehiclesOptimal] SizeRule: id={}, name='{}', maxWeight={}, maxLength={}, maxWidth={}, maxHeight={}",
                    rule.getId(), rule.getSizeRuleName(), rule.getMaxWeight(), 
                    rule.getMaxLength(), rule.getMaxWidth(), rule.getMaxHeight());
        }

        if (sortedsizeRules.isEmpty()) {
            log.error("[assignVehiclesOptimal] No vehicle rules found for category: id={}, name='{}'", categoryId, categoryName);
            throw new NotFoundException("No vehicle rules found for this category: " + categoryName, ErrorEnum.NOT_FOUND.getErrorCode());
        }

        // Log order detail sizes for debugging
        for (OrderDetailEntity detail : details) {
            OrderSizeEntity size = detail.getOrderSizeEntity();
            if (size != null) {
                log.info("[assignVehiclesOptimal] OrderDetail: id={}, weight={}, length={}, width={}, height={}",
                        detail.getId(), detail.getWeightTons(), size.getMaxLength(), size.getMaxWidth(), size.getMaxHeight());
            } else {
                log.warn("[assignVehiclesOptimal] OrderDetail: id={} has NULL OrderSizeEntity!", detail.getId());
            }
        }

        try {
            List<BinPacker.ContainerState> containers = BinPacker.pack(details, sortedsizeRules);
            List<ContractRuleAssignResponse> responses = BinPacker.toContractResponses(containers, details);
            
            long elapsedMs = (System.nanoTime() - t0) / 1_000_000;
            log.info("[assignVehiclesOptimal] SUCCESS - {} vehicles assigned in {}ms", responses.size(), elapsedMs);
            
            return responses;
        } catch (Exception e) {
            log.error("[assignVehiclesOptimal] BinPacker failed for orderId={}", orderId, e);
            throw e;
        }
    }

    /**
     * Tìm vehicle rule lớn hơn rule hiện tại trong sorted list
     */
    private SizeRuleEntity findNextBiggerRule(SizeRuleEntity current, List<SizeRuleEntity> sorted) {
        for (int i = 0; i < sorted.size(); i++) {
            if (sorted.get(i).getId().equals(current.getId()) && i + 1 < sorted.size()) {
                return sorted.get(i + 1);
            }
        }
        return null;
    }

    private OrderDetailForPackingResponse toPackingResponse(OrderDetailEntity entity) {
        return new OrderDetailForPackingResponse(
                entity.getId().toString(),
                entity.getWeightTons(),
                entity.getWeightBaseUnit(),
                entity.getUnit(),
                entity.getTrackingCode()
        );
    }

    private BigDecimal calculateTotalWeight(ContractRuleAssignResponse assignment, OrderDetailEntity newDetail) {
        return assignment.getCurrentLoad().add(newDetail.getWeightTons());
    }

    private SizeRuleEntity tryUpgrade(OrderDetailEntity detail,
                                      ContractRuleAssignResponse assignment,
                                      List<SizeRuleEntity> sortedRules) {

        int currentIdx = assignment.getVehicleIndex();

        for (int nextIdx = currentIdx + 1; nextIdx < sortedRules.size(); nextIdx++) {
            SizeRuleEntity nextRule = sortedRules.get(nextIdx);

            if (canFit(detail, nextRule, assignment)) {
                // cập nhật lại index cho assignment
                assignment.setVehicleIndex(nextIdx);
                return nextRule;
            }
        }
        return null;
    }

    @Override
    public PriceCalculationResponse calculateTotalPrice(ContractEntity contract,
                                                        BigDecimal distanceKm,
                                                        Map<UUID, Integer> vehicleCountMap) {
        final long t0 = System.nanoTime();

        if (contract.getOrderEntity() == null || contract.getOrderEntity().getCategory() == null) {
            throw new BadRequestException("Contract missing order/category", ErrorEnum.INVALID.getErrorCode());
        }

        log.info("🧮 Using unified pricing for contract calculation");
        
        BigDecimal total = BigDecimal.ZERO;
        List<PriceCalculationResponse.CalculationStep> steps = new ArrayList<>();

        for (Map.Entry<UUID, Integer> entry : vehicleCountMap.entrySet()) {
            UUID sizeRuleId = entry.getKey();
            int numOfVehicles = entry.getValue();

            SizeRuleEntity sizeRule = sizeRuleEntityService.findEntityById(sizeRuleId)
                    .orElseThrow(() -> new NotFoundException("Vehicle rule not found: " + sizeRuleId,
                            ErrorEnum.NOT_FOUND.getErrorCode()));

            // Use unified pricing service for consistent calculation
            UnifiedPricingService.UnifiedPriceResult pricingResult = unifiedPricingService.calculatePrice(
                    sizeRuleId, 
                    distanceKm, 
                    numOfVehicles, 
                    contract.getOrderEntity().getCategory().getId()
            );

            if (!pricingResult.isSuccess()) {
                throw new RuntimeException("Pricing calculation failed: " + pricingResult.getErrorMessage());
            }

            BigDecimal vehicleTotal = pricingResult.getTotalPrice();
            total = total.add(vehicleTotal);

            // Build calculation steps for each distance tier
            for (UnifiedPricingService.TierCalculationResult tierResult : pricingResult.getTierResults()) {
                steps.add(PriceCalculationResponse.CalculationStep.builder()
                        .sizeRuleName(sizeRule.getSizeRuleName())
                        .numOfVehicles(numOfVehicles)
                        .distanceRange(tierResult.getDistanceRange())
                        .unitPrice(tierResult.getUnitPrice())
                        .appliedKm(tierResult.getAppliedKm())
                        .subtotal(tierResult.getSubtotal().multiply(BigDecimal.valueOf(numOfVehicles)))
                        .build());
            }

            log.info("✅ Vehicle {} ({}x): {} VND with {} tiers", 
                    sizeRule.getSizeRuleName(), numOfVehicles, vehicleTotal, pricingResult.getTierResults().size());
        }

        // Get category adjustment values for response DTO (UnifiedPricingService already applied them)
        CategoryPricingDetailEntity adjustment = categoryPricingDetailEntityService.findByCategoryId(contract.getOrderEntity().getCategory().getId());
        BigDecimal categoryMultiplier = BigDecimal.ONE;
        BigDecimal categoryExtraFee = BigDecimal.ZERO;
        
        if (adjustment != null) {
            categoryMultiplier = adjustment.getPriceMultiplier() != null ? adjustment.getPriceMultiplier() : BigDecimal.ONE;
            categoryExtraFee = adjustment.getExtraFee() != null ? adjustment.getExtraFee() : BigDecimal.ZERO;
        }

        BigDecimal promotionDiscount = BigDecimal.ZERO;

        if (total.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Total price must not be negative");
        }

        // Calculate insurance fee
        OrderEntity order = contract.getOrderEntity();
        CategoryName categoryName = order.getCategory().getCategoryName();
        boolean isFragile = insuranceCalculationService.isFragileCategory(categoryName);
        boolean hasInsurance = Boolean.TRUE.equals(order.getHasInsurance());
        
        BigDecimal totalDeclaredValue = BigDecimal.ZERO;
        BigDecimal insuranceFee = BigDecimal.ZERO;
        // Use display rate for response (e.g., 0.15 = 0.15%), calculation uses decimal internally
        BigDecimal insuranceRateForDisplay = insuranceCalculationService.getInsuranceRateForDisplay(isFragile);
        BigDecimal vatRate = insuranceCalculationService.getVatRate();
        
        if (hasInsurance) {
            List<OrderDetailEntity> orderDetails = orderDetailEntityService
                    .findOrderDetailEntitiesByOrderEntityId(order.getId());
            totalDeclaredValue = insuranceCalculationService.calculateTotalDeclaredValue(orderDetails);
            insuranceFee = insuranceCalculationService.calculateTotalInsuranceFee(orderDetails, categoryName);
            log.info("🛡️ Insurance calculated: totalDeclaredValue={}, insuranceFee={}, rate={}%, isFragile={}", 
                    totalDeclaredValue, insuranceFee, insuranceRateForDisplay, isFragile);
        }
        
        // Grand total = transport fee + insurance fee
        BigDecimal grandTotal = total.add(insuranceFee);

        log.info("✅ Unified pricing calculation completed: {} VND (category: ×{} + {}, insurance: {})", 
                grandTotal, categoryMultiplier, categoryExtraFee, insuranceFee);

        return PriceCalculationResponse.builder()
                .totalPrice(total)
                .totalBeforeAdjustment(total) // Unified pricing already includes adjustments
                .categoryExtraFee(categoryExtraFee)
                .categoryMultiplier(categoryMultiplier)
                .promotionDiscount(promotionDiscount)
                .finalTotal(total)
                .totalDeclaredValue(totalDeclaredValue)
                .insuranceFee(insuranceFee)
                .insuranceRate(insuranceRateForDisplay) // Display rate as percentage (0.15 = 0.15%)
                .vatRate(vatRate)
                .hasInsurance(hasInsurance)
                .grandTotal(grandTotal)
                .steps(steps)
                .build();
    }

    private boolean canFit(OrderDetailEntity detail, SizeRuleEntity rule) {
        OrderSizeEntity size = detail.getOrderSizeEntity();
        if (size == null) return false;

        return detail.getWeightTons().compareTo(rule.getMaxWeight()) <= 0
                && size.getMaxLength().compareTo(rule.getMaxLength()) <= 0
                && size.getMaxWidth().compareTo(rule.getMaxWidth()) <= 0
                && size.getMaxHeight().compareTo(rule.getMaxHeight()) <= 0;
    }

    private boolean canFit(OrderDetailEntity detail, SizeRuleEntity rule, ContractRuleAssignResponse assignment) {
        OrderSizeEntity size = detail.getOrderSizeEntity();
        if (size == null) return false;

        BigDecimal newLoad = assignment.getCurrentLoad().add(detail.getWeightTons());
        return newLoad.compareTo(rule.getMaxWeight()) <= 0
                && size.getMaxLength().compareTo(rule.getMaxLength()) <= 0
                && size.getMaxWidth().compareTo(rule.getMaxWidth()) <= 0
                && size.getMaxHeight().compareTo(rule.getMaxHeight()) <= 0;
    }

    private boolean canFitAll(List<UUID> detailIds, SizeRuleEntity newRule, OrderDetailEntity newDetail) {
        BigDecimal totalWeight = newDetail.getWeightTons();

        for (UUID id : detailIds) {
            OrderDetailEntity d = orderDetailEntityService.findEntityById(id)
                    .orElseThrow(() -> new NotFoundException("Order detail not found: " + id, ErrorEnum.NOT_FOUND.getErrorCode()));

            totalWeight = totalWeight.add(d.getWeightTons());

            OrderSizeEntity size = d.getOrderSizeEntity();
            if (size == null) {
                log.warn("[canFitAll] Detail id={} missing size", id);
                return false;
            }

            if (size.getMaxLength().compareTo(newRule.getMaxLength()) > 0
                    || size.getMaxWidth().compareTo(newRule.getMaxWidth()) > 0
                    || size.getMaxHeight().compareTo(newRule.getMaxHeight()) > 0) {
                
                return false;
            }
        }

        boolean ok = totalWeight.compareTo(newRule.getMaxWeight()) <= 0;
        if (!ok) {
            
        }
        return ok;
    }

    @Override
    public BigDecimal calculateDistanceKm(AddressEntity from, AddressEntity to) {
        
        return vietMapDistanceService.calculateDistance(from, to);
    }

    @Override
    public BigDecimal calculateDistanceKm(AddressEntity from, AddressEntity to, String vehicleType) {
        
        return vietMapDistanceService.calculateDistance(from, to, vehicleType);
    }

    // CONTRACT TO CLOUD

    @Override
    public ContractResponse uploadContractFile(ContractFileUploadRequest req) throws IOException {

        // Get original filename and extension
        String originalFilename = req.file().getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        
        String fileName = "contract_" + UUID.randomUUID() + fileExtension;

        // upload Cloudinary
        var uploadResult = cloudinaryService.uploadFile(
                req.file().getBytes(),
                fileName,
                "CONTRACTS"
        );

        // Get the correct URL based on resource type
        String fileUrl;
        String resourceType = uploadResult.get("resource_type").toString();
        
        if ("raw".equals(resourceType)) {
            // For PDFs and other raw files, use getRawFileUrl
            String publicId = uploadResult.get("public_id").toString();
            fileUrl = cloudinaryService.getRawFileUrl(publicId);
            
        } else {
            // For images, use the secure_url from upload result
            fileUrl = uploadResult.get("secure_url").toString();
            
        }

        // load relationships
        ContractEntity ce = contractEntityService.findEntityById(req.contractId())
                .orElseThrow(() -> new RuntimeException("Contract not found by id: " + req.contractId()));

        // save DB
        ce.setAttachFileUrl(fileUrl);
        ce.setDescription(req.description());
        ce.setEffectiveDate(req.effectiveDate());
        ce.setExpirationDate(req.expirationDate());
        ce.setAdjustedValue(req.adjustedValue());
        ce.setContractName(req.contractName());
        
        // Set staff user ID from current authenticated user
        UUID staffUserId = userContextUtils.getCurrentUserId();
        UserEntity staffUser = new UserEntity();
        staffUser.setId(staffUserId);
        ce.setStaff(staffUser);

        var updated = contractEntityService.save(ce);

        // Update order status to CONTRACT_DRAFT
        if (ce.getOrderEntity() != null) {
            OrderEntity order = ce.getOrderEntity();
            OrderStatusEnum previousStatus = OrderStatusEnum.valueOf(order.getStatus());
            order.setStatus(OrderStatusEnum.CONTRACT_DRAFT.name());
            orderEntityService.save(order);

            // Send WebSocket notification for status change
            try {
                orderStatusWebSocketService.sendOrderStatusChange(
                    order.getId(),
                    order.getOrderCode(),
                    previousStatus,
                    OrderStatusEnum.CONTRACT_DRAFT
                );
                
            } catch (Exception e) {
                log.error("Failed to send WebSocket notification for order status change: {}", e.getMessage());
                // Don't throw - WebSocket failure shouldn't break business logic
            }
        }

        return contractMapper.toContractResponse(updated);

    }

    @Override
    @Transactional
    public ContractResponse generateAndSaveContractPdf(GenerateContractPdfRequest request) {
        log.info("[ContractService] Generating PDF for contract: {}", request.contractId());
        
        // Get contract entity
        ContractEntity contract = contractEntityService.findEntityById(request.contractId())
                .orElseThrow(() -> new NotFoundException(
                        "Contract not found: " + request.contractId(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        OrderEntity order = contract.getOrderEntity();
        if (order == null) {
            throw new BadRequestException(
                    "Contract has no associated order",
                    ErrorEnum.INVALID.getErrorCode()
            );
        }

        try {
            // Get vehicle assignment data for PDF
            List<ContractRuleAssignResponse> assignResult = assignVehiclesWithAvailability(order.getId());
            
            Map<UUID, Integer> vehicleCountMap = assignResult.stream()
                    .collect(Collectors.groupingBy(
                            ContractRuleAssignResponse::getSizeRuleId,
                            Collectors.summingInt(a -> 1)
                    ));

            BigDecimal distanceKm = distanceService.getDistanceInKilometers(order.getId());

            // Generate PDF using backend PdfGenerationService (handles pagination properly)
            byte[] pdfBytes = pdfGenerationService.generateContractPdf(
                    contract,
                    order,
                    assignResult,
                    distanceKm,
                    vehicleCountMap
            );

            // Upload to Cloudinary
            String fileName = "contract_" + request.contractName() + "_" + System.currentTimeMillis() + ".pdf";
            Map<String, Object> uploadResult = cloudinaryService.uploadFile(
                    pdfBytes,
                    fileName,
                    "CONTRACTS"
            );

            // Get the correct URL based on resource type
            String fileUrl;
            String resourceType = uploadResult.get("resource_type").toString();
            
            if ("raw".equals(resourceType)) {
                String publicId = uploadResult.get("public_id").toString();
                fileUrl = cloudinaryService.getRawFileUrl(publicId);
            } else {
                fileUrl = uploadResult.get("secure_url").toString();
            }

            // Update contract entity with PDF URL and metadata
            contract.setAttachFileUrl(fileUrl);
            contract.setContractName(request.contractName());
            contract.setEffectiveDate(request.effectiveDate());
            contract.setExpirationDate(request.expirationDate());
            contract.setAdjustedValue(request.adjustedValue());
            contract.setDescription(request.description());
            
            // Calculate total price for the contract
            PriceCalculationResponse totalPriceResponse = calculateTotalPrice(contract, distanceKm, vehicleCountMap);
            BigDecimal grandTotal = totalPriceResponse.getGrandTotal();
            contract.setTotalValue(grandTotal);
            
            // Set contract deadlines properly
            setContractDeadlines(contract, order);

            // Set staff user ID from current authenticated user
            UUID staffUserId = userContextUtils.getCurrentUserId();
            UserEntity staffUser = new UserEntity();
            staffUser.setId(staffUserId);
            contract.setStaff(staffUser);

            ContractEntity updated = contractEntityService.save(contract);

            // Update order status to CONTRACT_DRAFT
            OrderStatusEnum previousStatus = OrderStatusEnum.valueOf(order.getStatus());
            order.setStatus(OrderStatusEnum.CONTRACT_DRAFT.name());
            orderEntityService.save(order);

            // Send WebSocket notification for status change
            try {
                orderStatusWebSocketService.sendOrderStatusChange(
                        order.getId(),
                        order.getOrderCode(),
                        previousStatus,
                        OrderStatusEnum.CONTRACT_DRAFT
                );
            } catch (Exception e) {
                log.error("Failed to send WebSocket notification: {}", e.getMessage());
            }

            // Create CONTRACT_READY notification for customer when staff generates contract PDF
            try {
                // Get order details for package information
                List<OrderDetailEntity> orderDetails = orderDetailEntityService.findOrderDetailEntitiesByOrderEntityId(order.getId());
                
                String contractCode = updated.getContractName() != null ? updated.getContractName() : "HĐ-" + order.getOrderCode();
                
                // Get totalAmount from effective contract value (prioritizes adjustedValue over totalValue)
                double totalAmount = getEffectiveContractValue(updated);
                double depositAmount = 0.0;
                
                // Calculate deposit amount using contract settings
                try {
                    var contractSetting = contractSettingService.getLatestContractSetting();
                    if (contractSetting != null && contractSetting.depositPercent() != null) {
                        depositAmount = totalAmount * contractSetting.depositPercent().doubleValue() / 100.0;
                    } else {
                        // Fallback to 30% if no contract setting found
                        depositAmount = totalAmount * 0.1;
                    }
                } catch (Exception e) {
                    log.warn("Could not get contract setting for deposit calculation, using 30% default: {}", e.getMessage());
                    // Fallback to 30% if contract setting fails
                    depositAmount = totalAmount * 0.1;
                }
                
                log.info("📋 Contract notification - totalAmount: {}, depositAmount: {}, signingDeadline: {}, fullPaymentDeadline: {}",
                    totalAmount, depositAmount, updated.getSigningDeadline(), updated.getFullPaymentDeadline());
                
                CreateNotificationRequest notificationRequest = NotificationBuilder.buildContractReady(
                    order.getSender().getUser().getId(),
                    order.getOrderCode(),
                    contractCode,
                    depositAmount,
                    totalAmount,
                    updated.getSigningDeadline(),  // Use contract's signingDeadline (24h from now)
                    updated.getDepositPaymentDeadline(),  // Will be set after signing
                    orderDetails,
                    order.getId(),
                    updated.getId()
                );
                
                notificationService.createNotification(notificationRequest);
                log.info("✅ Created CONTRACT_READY notification for customer when staff generated PDF: {} with deposit: {}", order.getOrderCode(), depositAmount);
            } catch (Exception e) {
                log.error("❌ Failed to create CONTRACT_READY notification for PDF generation: {}", e.getMessage());
                // Don't throw - notification failure shouldn't break PDF generation
            }

            log.info("[ContractService] PDF generated and saved successfully for contract: {}", request.contractId());
            return contractMapper.toContractResponse(updated);

        } catch (Exception e) {
            log.error("[ContractService] Error generating PDF for contract {}: {}", request.contractId(), e.getMessage(), e);
            throw new BadRequestException(
                    "Failed to generate contract PDF: " + e.getMessage(),
                    ErrorEnum.INVALID.getErrorCode()
            );
        }
    }

    @Override
    public ContractResponse getContractByOrderId(UUID orderId) {
        
        ContractEntity contractEntity = contractEntityService.getContractByOrderId(orderId)
                .orElseThrow(() -> new NotFoundException(
                        "Contract not found for order ID: " + orderId,
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));
        return contractMapper.toContractResponse(contractEntity);
    }

    /**
     * Set contract deadlines based on order details and contract settings
     * Deadlines are configurable through contract settings:
     * - Effective date: Now (contract creation time)
     * - Expiration date: 1 year from effective date
     * - Contract signing: Uses signingDeadlineHours from contract settings
     * - Deposit payment: Uses depositDeadlineHours from contract settings (set when customer signs)
     * - Full payment: 1 day before pickup time (earliest estimated start time)
     */
    private void setContractDeadlines(ContractEntity contract, OrderEntity order) {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        
        // Get contract settings for deadline hours
        var contractSetting = contractSettingService.getLatestContractSetting();
        if (contractSetting == null) {
            throw new NotFoundException("Contract settings not found", ErrorEnum.NOT_FOUND.getErrorCode());
        }
        
        // Effective date: Contract creation time
        contract.setEffectiveDate(now);
        
        // Expiration date: 1 year from effective date
        contract.setExpirationDate(now.plusYears(1));
        
        // Signing deadline: Use signingDeadlineHours from contract settings
        contract.setSigningDeadline(now.plusHours(contractSetting.signingDeadlineHours()));
        
        // Deposit payment deadline: Will be set when customer signs the contract using depositDeadlineHours
        // Do NOT set here - will be set in signContractAndOrder() method
        
        // Full payment deadline: Use configurable days before pickup time from contract settings
        // Get the earliest estimated start time from order details
        java.time.LocalDateTime earliestPickupTime = order.getOrderDetailEntities().stream()
                .map(OrderDetailEntity::getEstimatedStartTime)
                .filter(time -> time != null)
                .min(java.time.LocalDateTime::compareTo)
                .orElse(now.plusDays(7)); // Default to 7 days if no estimated time
        
        // Set deadline using configurable days before pickup
        contract.setFullPaymentDeadline(earliestPickupTime.minusDays(contractSetting.fullPaymentDaysBeforePickup()));
    }

    @Override
    public List<StaffContractResponse> getAllContractsForStaff() {
        List<ContractEntity> contracts = contractEntityService.findAll();

        // Sort by createdAt DESC to show newest contracts first
        return contracts.stream()
                .sorted(Comparator.comparing(ContractEntity::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::mapToStaffContractResponse)
                .collect(Collectors.toList());
    }

    @Override
    public StaffContractResponse getContractDetailForStaff(UUID contractId) {
        ContractEntity contract = contractEntityService.findEntityById(contractId)
                .orElseThrow(() -> new NotFoundException(
                        "Contract not found: " + contractId,
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));
        
        return mapToStaffContractResponseWithTransactions(contract);
    }

    /**
     * Map ContractEntity to StaffContractResponse (for list view - without transactions)
     */
    private StaffContractResponse mapToStaffContractResponse(ContractEntity contract) {
        OrderEntity order = contract.getOrderEntity();
        UserEntity staff = contract.getStaff();
        
        // Calculate effective value (adjustedValue if > 0, else totalValue)
        BigDecimal effectiveValue = (contract.getAdjustedValue() != null && 
                contract.getAdjustedValue().compareTo(BigDecimal.ZERO) > 0) 
                ? contract.getAdjustedValue() 
                : contract.getTotalValue();
        
        // Get paid amount
        BigDecimal paidAmount = transactionEntityService.sumPaidAmountByContractId(contract.getId());
        if (paidAmount == null) paidAmount = BigDecimal.ZERO;
        
        // Calculate remaining
        BigDecimal remaining = effectiveValue != null ? effectiveValue.subtract(paidAmount) : BigDecimal.ZERO;
        if (remaining.compareTo(BigDecimal.ZERO) < 0) remaining = BigDecimal.ZERO;
        
        // Get sender info from CustomerEntity -> UserEntity
        String senderName = null;
        String senderPhone = null;
        if (order != null && order.getSender() != null && order.getSender().getUser() != null) {
            senderName = order.getSender().getUser().getFullName();
            senderPhone = order.getSender().getUser().getPhoneNumber();
        }
        
        return StaffContractResponse.builder()
                .id(contract.getId())
                .contractName(contract.getContractName())
                .status(contract.getStatus())
                .description(contract.getDescription())
                .attachFileUrl(contract.getAttachFileUrl())
                .createdAt(contract.getCreatedAt())
                .effectiveDate(contract.getEffectiveDate())
                .expirationDate(contract.getExpirationDate())
                .signingDeadline(contract.getSigningDeadline())
                .depositPaymentDeadline(contract.getDepositPaymentDeadline())
                .fullPaymentDeadline(contract.getFullPaymentDeadline())
                .totalValue(contract.getTotalValue())
                .adjustedValue(contract.getAdjustedValue())
                .effectiveValue(effectiveValue)
                .paidAmount(paidAmount)
                .remainingAmount(remaining)
                .order(order != null ? StaffContractResponse.OrderInfo.builder()
                        .id(order.getId())
                        .orderCode(order.getOrderCode())
                        .status(order.getStatus())
                        .senderName(senderName)
                        .senderPhone(senderPhone)
                        .receiverName(order.getReceiverName())
                        .receiverPhone(order.getReceiverPhone())
                        .pickupAddress(order.getPickupAddress() != null ? buildAddressString(order.getPickupAddress()) : null)
                        .deliveryAddress(order.getDeliveryAddress() != null ? buildAddressString(order.getDeliveryAddress()) : null)
                        .createdAt(order.getCreatedAt())
                        .build() : null)
                .staff(staff != null ? StaffContractResponse.StaffInfo.builder()
                        .id(staff.getId())
                        .fullName(staff.getFullName())
                        .email(staff.getEmail())
                        .phoneNumber(staff.getPhoneNumber())
                        .build() : null)
                .build();
    }

    /**
     * Map ContractEntity to StaffContractResponse with transactions (for detail view)
     */
    private StaffContractResponse mapToStaffContractResponseWithTransactions(ContractEntity contract) {
        StaffContractResponse response = mapToStaffContractResponse(contract);
        
        // Get transactions
        List<TransactionEntity> transactions = transactionEntityService.findByContractId(contract.getId());
        
        List<StaffContractResponse.TransactionInfo> transactionInfos = transactions.stream()
                .map(tx -> StaffContractResponse.TransactionInfo.builder()
                        .id(tx.getId())
                        .transactionType(tx.getTransactionType())
                        .amount(tx.getAmount())
                        .status(tx.getStatus())
                        .paymentProvider(tx.getPaymentProvider())
                        .currencyCode(tx.getCurrencyCode())
                        .paymentDate(tx.getPaymentDate())
                        .createdAt(tx.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
        
        response.setTransactions(transactionInfos);
        
        return response;
    }

    /**
     * Build full address string from AddressEntity fields
     */
    private String buildAddressString(AddressEntity address) {
        if (address == null) return null;
        
        StringBuilder sb = new StringBuilder();
        if (address.getStreet() != null && !address.getStreet().isEmpty()) {
            sb.append(address.getStreet());
        }
        if (address.getWard() != null && !address.getWard().isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(address.getWard());
        }
        if (address.getProvince() != null && !address.getProvince().isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(address.getProvince());
        }
        return sb.length() > 0 ? sb.toString() : null;
    }
}
