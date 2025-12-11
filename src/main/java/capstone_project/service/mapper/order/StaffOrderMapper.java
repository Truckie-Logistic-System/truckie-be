package capstone_project.service.mapper.order;

import capstone_project.dtos.response.issue.GetIssueImageResponse;
import capstone_project.dtos.response.issue.SimpleIssueResponse;
import capstone_project.dtos.response.issue.SimpleStaffResponse;
import capstone_project.dtos.response.order.*;
import capstone_project.dtos.response.order.contract.ContractResponse;
import capstone_project.dtos.response.order.contract.SimpleContractResponse;
import capstone_project.dtos.response.order.seal.GetSealResponse;
import capstone_project.dtos.response.order.transaction.SimpleTransactionResponse;
import capstone_project.dtos.response.order.transaction.TransactionResponse;
import capstone_project.dtos.response.order.PhotoCompletionResponse;
import capstone_project.dtos.response.vehicle.VehicleAssignmentResponse;
import capstone_project.entity.auth.UserEntity;
import capstone_project.entity.user.driver.DriverEntity;
import capstone_project.entity.user.driver.PenaltyHistoryEntity;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.repository.entityServices.auth.UserEntityService;
import capstone_project.repository.entityServices.order.VehicleFuelConsumptionEntityService;
import capstone_project.repository.entityServices.setting.ContractSettingEntityService;
import capstone_project.repository.entityServices.user.DriverEntityService;
import capstone_project.repository.entityServices.user.PenaltyHistoryEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleAssignmentEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleEntityService;
import capstone_project.repository.entityServices.order.order.OrderDetailEntityService;
import capstone_project.entity.issue.IssueEntity;
import capstone_project.entity.order.order.OrderDetailEntity;
import capstone_project.repository.entityServices.issue.IssueEntityService;
import capstone_project.service.services.issue.IssueImageService;
import capstone_project.service.services.order.order.JourneyHistoryService;
import capstone_project.service.services.order.seal.SealService;
import capstone_project.service.services.order.order.PhotoCompletionService;
import capstone_project.service.services.pricing.PricingUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class StaffOrderMapper {

    private final PenaltyHistoryEntityService penaltyHistoryEntityService;
    private final VehicleFuelConsumptionEntityService vehicleFuelConsumptionEntityService;
    private final UserEntityService userEntityService;
    private final DriverEntityService driverEntityService;
    private final VehicleEntityService vehicleEntityService;
    private final SealService sealService;
    private final VehicleAssignmentEntityService vehicleAssignmentEntityService;
    private final JourneyHistoryService journeyHistoryService;
    private final IssueEntityService issueEntityService;
    private final IssueImageService issueImageService;
    // PhotoCompletionService may not exist in every project; if not present remove this field and helper below
    private final PhotoCompletionService photoCompletionService;
    private final ContractSettingEntityService contractSettingEntityService;
    private final OrderDetailEntityService orderDetailEntityService;
    private final capstone_project.repository.entityServices.device.DeviceEntityService deviceEntityService;

    /**
     * Translate status to Vietnamese
     */
    private String translateStatusToVietnamese(String status, String type) {
        if (status == null) return "Không xác định";
        
        switch (type.toUpperCase()) {
            case "ORDER":
                return switch (status.toUpperCase()) {
                    case "PENDING" -> "Chờ xử lý";
                    case "PROCESSING" -> "Đang xử lý";
                    case "CANCELLED" -> "Đã hủy";
                    case "CONTRACT_DRAFT" -> "Hợp đồng nháp";
                    case "CONTRACT_SIGNED" -> "Đã ký hợp đồng";
                    case "ON_PLANNING" -> "Đang lên kế hoạch";
                    case "ASSIGNED_TO_DRIVER" -> "Đã phân công tài xế";
                    case "FULLY_PAID" -> "Đã thanh toán đủ";
                    case "PICKING_UP" -> "Đang lấy hàng";
                    case "ON_DELIVERED" -> "Đang giao hàng";
                    case "ONGOING_DELIVERED" -> "Đang trong quá trình giao";
                    case "IN_TROUBLES" -> "Gặp sự cố";
                    case "COMPENSATION" -> "Bồi thường";
                    case "DELIVERED" -> "Đã giao hàng";
                    case "SUCCESSFUL" -> "Hoàn thành thành công";
                    case "RETURNING" -> "Đang trả hàng";
                    case "RETURNED" -> "Đã trả hàng";
                    default -> status;
                };
            case "VEHICLE_ASSIGNMENT":
                return switch (status.toUpperCase()) {
                    case "PENDING" -> "Chờ xử lý";
                    case "ASSIGNED" -> "Đã phân công";
                    case "IN_PROGRESS" -> "Đang thực hiện";
                    case "COMPLETED" -> "Đã hoàn thành";
                    case "CANCELLED" -> "Đã hủy";
                    default -> status;
                };
            case "ORDER_DETAIL":
                return switch (status.toUpperCase()) {
                    case "PENDING" -> "Chờ xử lý";
                    case "ON_PLANNING" -> "Đang lên kế hoạch";
                    case "ASSIGNED_TO_DRIVER" -> "Đã phân công tài xế";
                    case "PICKING_UP" -> "Đang lấy hàng";
                    case "ON_DELIVERED" -> "Đang giao hàng";
                    case "ONGOING_DELIVERED" -> "Đang trong quá trình giao";
                    case "DELIVERED" -> "Đã giao hàng";
                    case "SUCCESSFUL" -> "Hoàn thành thành công";
                    case "IN_TROUBLES" -> "Gặp sự cố";
                    case "COMPENSATION" -> "Bồi thường";
                    case "RETURNING" -> "Đang trả hàng";
                    case "RETURNED" -> "Đã trả hàng";
                    case "CANCELLED" -> "Đã hủy";
                    default -> status;
                };
            default:
                return status;
        }
    }

    public OrderForStaffResponse toStaffOrderForStaffResponse(
            GetOrderResponse orderResponse,
            ContractResponse contractResponse,
            List<TransactionResponse> transactionResponses
    ) {
        BigDecimal effectiveTotal = null;
        try {
            if (contractResponse != null) {
                BigDecimal contractTotal = contractResponse.totalValue();
                if (contractTotal != null && contractTotal.compareTo(BigDecimal.ZERO) > 0) {
                    effectiveTotal = contractTotal;
                } else {
                    effectiveTotal = contractResponse.adjustedValue();
                }
            }
        } catch (Exception ignored) {
        }

        // Calculate deposit amount based on contract adjusted value
        BigDecimal adjustedValue = contractResponse != null ? contractResponse.adjustedValue() : null;
        BigDecimal depositAmount = calculateDepositAmount(effectiveTotal, adjustedValue);

        // Convert Order with enhanced information for staff
        StaffOrderResponse staffOrderResponse = toStaffOrderResponseWithEnhancedInfo(orderResponse, depositAmount);

        // Convert Contract
        SimpleContractResponse simpleContractResponse = contractResponse != null ?
                toSimpleContractResponse(contractResponse) : null;

        // Convert Transactions
        List<SimpleTransactionResponse> simpleTransactionResponses = transactionResponses.stream()
                .map(this::toSimpleTransactionResponse)
                .collect(Collectors.toList());

        return new OrderForStaffResponse(
                staffOrderResponse,
                simpleContractResponse,
                simpleTransactionResponses
        );
    }

    private StaffOrderResponse toStaffOrderResponseWithEnhancedInfo(GetOrderResponse response, BigDecimal depositAmount) {
        String deliveryAddress = combineAddress(
                response.deliveryAddress().street(),
                response.deliveryAddress().ward(),
                response.deliveryAddress().province()
        );

        String pickupAddress = combineAddress(
                response.pickupAddress().street(),
                response.pickupAddress().ward(),
                response.pickupAddress().province()
        );

        // Collect unique vehicle assignment IDs from order details
        Set<UUID> uniqueVehicleAssignmentIds = response.orderDetails().stream()
                .map(GetOrderDetailResponse::vehicleAssignmentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Build full vehicle assignment responses for unique IDs
        List<StaffVehicleAssignmentResponse> vehicleAssignments = uniqueVehicleAssignmentIds.stream()
                .map(vaId -> {
                    // Find the first vehicleAssignmentResponse with this ID from response.vehicleAssignments()
                    VehicleAssignmentResponse vaResponse = response.vehicleAssignments().stream()
                            .filter(va -> va.id().equals(vaId))
                            .findFirst()
                            .orElse(null);
                    if (vaResponse != null) {
                        // Fetch issues and photo completions for this vehicle assignment
                        List<SimpleIssueResponse> issues = getIssues(vaId);
                        List<String> photoCompletions = getPhotoCompletionsByVehicleAssignmentId(vaId);
                        return toStaffVehicleAssignmentResponse(vaResponse, issues, photoCompletions);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // Process order details with only vehicle assignment ID reference
        List<StaffOrderDetailResponse> staffOrderDetails = response.orderDetails().stream()
                .map(this::toStaffOrderDetailResponseWithEnhancedInfo)
                .collect(Collectors.toList());

        return new StaffOrderResponse(
                response.id(),
                depositAmount,
                response.notes(),
                response.totalQuantity(),
                response.orderCode(),
                response.receiverName(),
                response.receiverPhone(),
                response.receiverIdentity(),
                response.packageDescription(),
                response.createdAt(),
                response.status(),
                deliveryAddress,
                pickupAddress,
                response.sender().getRepresentativeName(),
                response.sender().getRepresentativePhone(),
                response.sender().getCompanyName(),
                response.category().categoryName().name(),
                response.category().description(), // Add category description
                response.hasInsurance(),
                response.totalInsuranceFee(),
                response.totalDeclaredValue(),
                staffOrderDetails,
                vehicleAssignments  // Add aggregated vehicle assignments
        );
    }

    private StaffOrderDetailResponse toStaffOrderDetailResponseWithEnhancedInfo(GetOrderDetailResponse detail) {
        SimpleOrderSizeResponse orderSize = null;
        if (detail.orderSizeId() != null) {
            orderSize = new SimpleOrderSizeResponse(
                    detail.orderSizeId().id().toString(),
                    detail.orderSizeId().description(),
                    detail.orderSizeId().minLength(),
                    detail.orderSizeId().maxLength(),
                    detail.orderSizeId().minHeight(),
                    detail.orderSizeId().maxHeight(),
                    detail.orderSizeId().minWidth(),
                    detail.orderSizeId().maxWidth()
            );
        }

        return new StaffOrderDetailResponse(
                detail.trackingCode(),
                detail.weightBaseUnit(),
                detail.unit(),
                detail.description(),
                detail.status(),
                detail.startTime(),
                detail.estimatedStartTime(),
                detail.endTime(),
                detail.estimatedEndTime(),
                detail.createdAt(),
                detail.trackingCode(),
                orderSize,
                detail.vehicleAssignmentId()  // Only store ID reference
        );
    }

    /**
     * Build StaffVehicleAssignmentFullResponse from entity for standalone vehicle assignment detail page
     * Includes order info and order details
     */
    public StaffVehicleAssignmentFullResponse toStaffVehicleAssignmentFullResponse(
            VehicleAssignmentEntity entity,
            VehicleAssignmentResponse basicResponse) {
        UUID vehicleAssignmentId = entity.getId();
        
        // Get issues and photo completions
        List<SimpleIssueResponse> issues = getIssues(vehicleAssignmentId);
        List<String> photoCompletions = getPhotoCompletionsByVehicleAssignmentId(vehicleAssignmentId);
        
        // Build base response
        StaffVehicleAssignmentResponse baseResponse = toStaffVehicleAssignmentResponse(basicResponse, issues, photoCompletions);
        
        // Get order details for this vehicle assignment
        List<StaffOrderDetailResponse> orderDetails = getOrderDetailsByVehicleAssignmentId(vehicleAssignmentId);
        
        // Get order info from the first order detail (all order details belong to the same order)
        SimpleOrderInfo orderInfo = getOrderInfoByVehicleAssignmentId(vehicleAssignmentId);
        
        // Translate vehicle assignment status to Vietnamese
        String translatedVehicleAssignmentStatus = translateStatusToVietnamese(baseResponse.status(), "VEHICLE_ASSIGNMENT");
        
        // Translate order status to Vietnamese
        SimpleOrderInfo translatedOrderInfo = orderInfo != null ? 
                new SimpleOrderInfo(
                        orderInfo.id(),
                        orderInfo.orderCode(),
                        translateStatusToVietnamese(orderInfo.status(), "ORDER"),
                        orderInfo.createdAt(),
                        orderInfo.receiverName(),
                        orderInfo.receiverPhone(),
                        orderInfo.deliveryAddress(),
                        orderInfo.pickupAddress(),
                        orderInfo.senderName(),
                        orderInfo.companyName()
                ) : null;
        
        // Translate order detail statuses to Vietnamese
        List<StaffOrderDetailResponse> translatedOrderDetails = orderDetails.stream()
                .map(detail -> new StaffOrderDetailResponse(
                        detail.trackingCode(),
                        detail.weightBaseUnit(),
                        detail.unit(),
                        detail.description(),
                        translateStatusToVietnamese(detail.status(), "ORDER_DETAIL"),
                        detail.startTime(),
                        detail.estimatedStartTime(),
                        detail.endTime(),
                        detail.estimatedEndTime(),
                        detail.createdAt(),
                        detail.trackingCode(),
                        detail.orderSize(),
                        detail.vehicleAssignmentId()
                ))
                .collect(Collectors.toList());
        
        // Get device info from device_ids field
        List<StaffVehicleAssignmentFullResponse.DeviceInfo> devices = getDeviceInfoFromAssignment(entity);
        
        return new StaffVehicleAssignmentFullResponse(
                baseResponse.id(),
                baseResponse.vehicle(),
                baseResponse.primaryDriver(),
                baseResponse.secondaryDriver(),
                translatedVehicleAssignmentStatus,
                baseResponse.trackingCode(),
                entity.getDescription(),
                baseResponse.penalties(),
                baseResponse.fuelConsumption(),
                baseResponse.seals(),
                baseResponse.journeyHistories(),
                baseResponse.photoCompletions(),
                baseResponse.issues(),
                translatedOrderDetails,
                translatedOrderInfo,
                devices
        );
    }
    
    private List<StaffOrderDetailResponse> getOrderDetailsByVehicleAssignmentId(UUID vehicleAssignmentId) {
        if (vehicleAssignmentId == null) return Collections.emptyList();
        
        try {
            List<OrderDetailEntity> orderDetails = orderDetailEntityService.findByVehicleAssignmentId(vehicleAssignmentId);
            if (orderDetails == null || orderDetails.isEmpty()) return Collections.emptyList();
            
            return orderDetails.stream()
                    .map(detail -> new StaffOrderDetailResponse(
                            detail.getTrackingCode(),
                            detail.getWeightBaseUnit(),
                            detail.getUnit(),
                            detail.getDescription(),
                            detail.getStatus(),
                            detail.getStartTime(),
                            detail.getEstimatedStartTime(),
                            detail.getEndTime(),
                            detail.getEstimatedEndTime(),
                            detail.getCreatedAt(),
                            detail.getTrackingCode(),
                            detail.getOrderSizeEntity() != null ? new SimpleOrderSizeResponse(
                                    detail.getOrderSizeEntity().getId().toString(),
                                    detail.getOrderSizeEntity().getDescription(),
                                    detail.getOrderSizeEntity().getMinLength(),
                                    detail.getOrderSizeEntity().getMaxLength(),
                                    detail.getOrderSizeEntity().getMinHeight(),
                                    detail.getOrderSizeEntity().getMaxHeight(),
                                    detail.getOrderSizeEntity().getMinWidth(),
                                    detail.getOrderSizeEntity().getMaxWidth()
                            ) : null,
                            vehicleAssignmentId
                    ))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Could not fetch order details for vehicle assignment {}: {}", vehicleAssignmentId, e.getMessage());
            return Collections.emptyList();
        }
    }
    
    private SimpleOrderInfo getOrderInfoByVehicleAssignmentId(UUID vehicleAssignmentId) {
        if (vehicleAssignmentId == null) return null;
        
        try {
            List<OrderDetailEntity> orderDetails = orderDetailEntityService.findByVehicleAssignmentId(vehicleAssignmentId);
            if (orderDetails == null || orderDetails.isEmpty()) return null;
            
            // Get order from first order detail
            var firstOrderDetail = orderDetails.get(0);
            var order = firstOrderDetail.getOrderEntity();
            
            if (order == null) return null;
            
            String deliveryAddress = combineAddress(
                    order.getDeliveryAddress() != null ? order.getDeliveryAddress().getStreet() : null,
                    order.getDeliveryAddress() != null ? order.getDeliveryAddress().getWard() : null,
                    order.getDeliveryAddress() != null ? order.getDeliveryAddress().getProvince() : null
            );
            
            String pickupAddress = combineAddress(
                    order.getPickupAddress() != null ? order.getPickupAddress().getStreet() : null,
                    order.getPickupAddress() != null ? order.getPickupAddress().getWard() : null,
                    order.getPickupAddress() != null ? order.getPickupAddress().getProvince() : null
            );
            
            String senderName = order.getSender() != null ? order.getSender().getRepresentativeName() : null;
            String companyName = order.getSender() != null ? order.getSender().getCompanyName() : null;
            
            return new SimpleOrderInfo(
                    order.getId(),
                    order.getOrderCode(),
                    order.getStatus(),
                    order.getCreatedAt(),
                    order.getReceiverName(),
                    order.getReceiverPhone(),
                    deliveryAddress,
                    pickupAddress,
                    senderName,
                    companyName
            );
        } catch (Exception e) {
            log.warn("Could not fetch order info for vehicle assignment {}: {}", vehicleAssignmentId, e.getMessage());
            return null;
        }
    }

    private StaffVehicleAssignmentResponse toStaffVehicleAssignmentResponse(
            VehicleAssignmentResponse vehicleAssignmentResponse,
            List<SimpleIssueResponse> issues,
            List<String> photoCompletions) {
        UUID vehicleAssignmentId = vehicleAssignmentResponse.id();

        VehicleAssignmentEntity vehicleAssignmentEntity = null;
        try {
            vehicleAssignmentEntity = vehicleAssignmentEntityService.findEntityById(vehicleAssignmentId).orElse(null);
            if (vehicleAssignmentEntity == null) {
                log.warn("Vehicle assignment entity not found in database for ID: {}", vehicleAssignmentId);
            }
        } catch (Exception e) {
            log.error("Error retrieving vehicle assignment entity from database: {}", e.getMessage(), e);
        }

        List<PenaltyHistoryResponse> penalties = getPenaltiesByVehicleAssignmentId(vehicleAssignmentId);

        VehicleFuelConsumptionResponse fuelConsumption = getFuelConsumptionByVehicleAssignmentId(vehicleAssignmentId);
        // Use photoCompletions from parameter (already fetched by processVehicleAssignments)
        if (photoCompletions == null) photoCompletions = Collections.emptyList();
        List<SimpleIssueResponse> issuesList = issues != null ? issues : Collections.emptyList();

        StaffDriverResponse primaryDriver = null;
        StaffDriverResponse secondaryDriver = null;

        try {
            UUID primaryDriverUserId = null;
            UUID secondaryDriverUserId = null;

            if (vehicleAssignmentEntity != null) {
                if (vehicleAssignmentEntity.getDriver1() != null && vehicleAssignmentEntity.getDriver1().getUser() != null) {
                    primaryDriverUserId = vehicleAssignmentEntity.getDriver1().getUser().getId();
                }
                if (vehicleAssignmentEntity.getDriver2() != null && vehicleAssignmentEntity.getDriver2().getUser() != null) {
                    secondaryDriverUserId = vehicleAssignmentEntity.getDriver2().getUser().getId();
                }
            }

            if (primaryDriverUserId == null && vehicleAssignmentResponse.driver_id_1() != null) {
                primaryDriverUserId = vehicleAssignmentResponse.driver_id_1();
            }

            if (secondaryDriverUserId == null && vehicleAssignmentResponse.driver_id_2() != null) {
                secondaryDriverUserId = vehicleAssignmentResponse.driver_id_2();
            }

            if (primaryDriverUserId != null) {
                primaryDriver = getEnhancedDriverInfo(primaryDriverUserId);
            }

            if (secondaryDriverUserId != null) {
                secondaryDriver = getEnhancedDriverInfo(secondaryDriverUserId);
            }
        } catch (Exception e) {
            log.warn("Could not fetch driver info for vehicle assignment {}: {}", vehicleAssignmentId, e.getMessage());
        }

        VehicleResponse vehicle = null;
        try {
            UUID vehicleId = null;

            if (vehicleAssignmentEntity != null && vehicleAssignmentEntity.getVehicleEntity() != null) {
                vehicleId = vehicleAssignmentEntity.getVehicleEntity().getId();
            } else if (vehicleAssignmentResponse.vehicleId() != null) {
                vehicleId = vehicleAssignmentResponse.vehicleId();
            }

            if (vehicleId != null) {
                var vehicleEntity = vehicleEntityService.findEntityById(vehicleId).orElse(null);
                if (vehicleEntity != null) {
                    vehicle = new VehicleResponse(
                            vehicleEntity.getId(),
                            vehicleEntity.getManufacturer(),
                            vehicleEntity.getModel(),
                            vehicleEntity.getLicensePlateNumber(),
                            vehicleEntity.getVehicleTypeEntity() != null ? vehicleEntity.getVehicleTypeEntity().getVehicleTypeName() : null,
                            vehicleEntity.getVehicleTypeEntity() != null ? vehicleEntity.getVehicleTypeEntity().getDescription() : null
                    );
                } else {
                    log.warn("Vehicle entity not found for ID: {}", vehicleId);
                }
            } else {
                log.warn("Vehicle ID is null in both entity and response for assignment: {}", vehicleAssignmentId);
            }
        } catch (Exception e) {
            log.warn("Could not fetch vehicle info for vehicle assignment {}: {}", vehicleAssignmentId, e.getMessage());
        }

        // Retrieve journey history for the vehicle assignment (null-safe)
        List<JourneyHistoryResponse> journeyHistories = Collections.emptyList();
        try {
            List<JourneyHistoryResponse> raw = journeyHistoryService.getByVehicleAssignmentId(vehicleAssignmentId);
            if (raw != null) journeyHistories = raw;
        } catch (Exception e) {
            log.warn("Could not fetch journey histories for {}: {}", vehicleAssignmentId, e.getMessage());
        }

        // Get order seals (null-safe)
        List<GetSealResponse> seals = Collections.emptyList();
        try {
            List<GetSealResponse> raw = sealService.getAllSealsByVehicleAssignmentId(vehicleAssignmentId);
            if (raw != null) seals = raw;
        } catch (Exception e) {
            log.warn("Could not fetch order seals for {}: {}", vehicleAssignmentId, e.getMessage());
        }

        String status = vehicleAssignmentEntity != null ? vehicleAssignmentEntity.getStatus() : vehicleAssignmentResponse.status();
        String trackingCode = vehicleAssignmentResponse.trackingCode();

        return new StaffVehicleAssignmentResponse(
                vehicleAssignmentId,
                vehicle,
                primaryDriver,
                secondaryDriver,
                status,
                trackingCode,
                penalties,
                fuelConsumption,
                seals,
                journeyHistories,
                photoCompletions,
                issuesList
        );
    }

    private StaffDriverResponse getEnhancedDriverInfo(UUID driverId) {
        if (driverId == null) return null;

        try {
            UserEntity userEntity = userEntityService.findEntityById(driverId).orElse(null);
            if (userEntity == null) {
                log.warn("User entity not found for driver ID: {}", driverId);
                return null;
            }

            DriverEntity driverEntity = driverEntityService.findByUserId(driverId).orElse(null);

            return new StaffDriverResponse(
                    null,
                    userEntity.getFullName(),
                    userEntity.getPhoneNumber(),
                    userEntity.getEmail(),
                    userEntity.getImageUrl(),
                    userEntity.getGender(),
                    userEntity.getDateOfBirth(),
                    driverEntity != null ? driverEntity.getIdentityNumber() : null,
                    driverEntity != null ? driverEntity.getDriverLicenseNumber() : null,
                    driverEntity != null ? driverEntity.getCardSerialNumber() : null,
                    driverEntity != null ? driverEntity.getPlaceOfIssue() : null,
                    driverEntity != null ? driverEntity.getDateOfIssue() : null,
                    driverEntity != null ? driverEntity.getDateOfExpiry() : null,
                    driverEntity != null ? driverEntity.getLicenseClass() : null,
                    driverEntity != null ? driverEntity.getDateOfPassing() : null,
                    userEntity.getStatus(),
                    null,
                    userEntity.getCreatedAt()
            );
        } catch (Exception e) {
            log.error("Could not fetch enhanced driver info for driver ID {}: {}", driverId, e.getMessage(), e);
            return null;
        }
    }

    private List<PenaltyHistoryResponse> getPenaltiesByVehicleAssignmentId(UUID vehicleAssignmentId) {
        if (vehicleAssignmentId == null) return new ArrayList<>();

        try {
            List<PenaltyHistoryEntity> penalties = penaltyHistoryEntityService.findByVehicleAssignmentId(vehicleAssignmentId);
            return penalties.stream()
                    .map(entity -> {
                        // Get driver information if available
                        StaffDriverResponse driver = null;
                        if (entity.getIssueBy() != null && entity.getIssueBy().getUser() != null) {
                            driver = getEnhancedDriverInfo(entity.getIssueBy().getUser().getId());
                        }

                        return new PenaltyHistoryResponse(
                                entity.getId(),
                                entity.getViolationType(),
                                null, // violationDescription - not available in entity
                                null, // penaltyAmount - not available in entity
                                entity.getPenaltyDate(),
                                null, // location - not available in entity
                                null, // status - not available in entity
                                null, // paymentDate - not available in entity
                                null, // disputeReason - not available in entity
                                driver
                        );
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Could not fetch penalties for vehicle assignment {}: {}", vehicleAssignmentId, e.getMessage());
            return new ArrayList<>();
        }
    }

    private VehicleFuelConsumptionResponse getFuelConsumptionByVehicleAssignmentId(UUID vehicleAssignmentId) {
        if (vehicleAssignmentId == null) return null;

        try {
            return vehicleFuelConsumptionEntityService.findByVehicleAssignmentId(vehicleAssignmentId)
                    .map(entity -> new VehicleFuelConsumptionResponse(
                            entity.getId(),
                            entity.getOdometerReadingAtStart(),
                            entity.getOdometerAtStartUrl(),
                            entity.getOdometerReadingAtEnd(),
                            entity.getOdometerAtEndUrl(),
                            entity.getDistanceTraveled(),
                            entity.getDateRecorded(),
                            entity.getNotes(),
                            entity.getFuelVolume(),
                            entity.getCompanyInvoiceImageUrl()
                    ))
                    .orElse(null);
        } catch (Exception e) {
            log.warn("Could not fetch fuel consumption for vehicle assignment {}: {}", vehicleAssignmentId, e.getMessage());
            return null;
        }
    }

    private SimpleContractResponse toSimpleContractResponse(ContractResponse contract) {
        if (contract == null) return null;

        String staffName = "";
        try {
            if (contract.staffId() != null) {
                UserEntity staff = userEntityService.findEntityById(UUID.fromString(contract.staffId())).orElse(null);
                if (staff != null) {
                    staffName = staff.getFullName();
                }
            }
        } catch (Exception e) {
            log.warn("Could not fetch staff name for contract: {}", e.getMessage());
        }

        return new SimpleContractResponse(
                contract.id(),
                contract.contractName(),
                contract.effectiveDate(),
                contract.expirationDate(),
                contract.totalValue(),
                contract.adjustedValue(),
                contract.description(),
                contract.attachFileUrl(),
                contract.status(),
                staffName
        );
    }

    private SimpleTransactionResponse toSimpleTransactionResponse(TransactionResponse transaction) {
        if (transaction == null) return null;

        return new SimpleTransactionResponse(
                transaction.id(),
                transaction.paymentProvider(),
                transaction.gatewayOrderCode() != null ? transaction.gatewayOrderCode().toString() : null,
                transaction.amount(),
                transaction.currencyCode(),
                transaction.status(),
                transaction.paymentDate(),
                transaction.transactionType()
        );
    }

    private String combineAddress(String street, String ward, String province) {
        StringBuilder address = new StringBuilder();
        if (street != null && !street.isEmpty()) {
            address.append(street);
        }
        if (ward != null && !ward.isEmpty()) {
            if (!address.isEmpty()) address.append(", ");
            address.append(ward);
        }
        if (province != null && !province.isEmpty()) {
            if (!address.isEmpty()) address.append(", ");
            address.append(province);
        }
        return address.toString();
    }

    private List<String> getPhotoCompletionsByVehicleAssignmentId(UUID vehicleAssignmentId) {
        if (vehicleAssignmentId == null) return Collections.emptyList();
        try {
            return photoCompletionService.getByVehicleAssignmentId(vehicleAssignmentId).stream()
                    .map(PhotoCompletionResponse::imageUrl)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Could not fetch photo completions for vehicle assignment {}: {}", vehicleAssignmentId, e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<SimpleIssueResponse> getIssues(UUID vehicleAssignmentId) {
        if (vehicleAssignmentId == null) return Collections.emptyList();

        try {
            // Get vehicle assignment entity
            VehicleAssignmentEntity vehicleAssignment = vehicleAssignmentEntityService
                    .findEntityById(vehicleAssignmentId)
                    .orElse(null);
            
            if (vehicleAssignment == null) {
                log.warn("Vehicle assignment not found for ID: {}", vehicleAssignmentId);
                return Collections.emptyList();
            }
            
            // Get ALL issues for this vehicle assignment
            List<IssueEntity> issues = issueEntityService.findAllByVehicleAssignmentEntity(vehicleAssignment);
            
            if (issues == null || issues.isEmpty()) {
                return Collections.emptyList();
            }
            
            // Convert each issue to SimpleIssueResponse with images
            List<SimpleIssueResponse> simpleIssues = new ArrayList<>();
            for (IssueEntity issue : issues) {
                try {
                    // Get issue images for this issue
                    GetIssueImageResponse issueWithImages = issueImageService.getImage(issue.getId());
                    if (issueWithImages != null) {
                        SimpleIssueResponse simple = safeToSimpleIssueResponse(issueWithImages);
                        if (simple != null) {
                            simpleIssues.add(simple);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Error converting issue {} to SimpleIssueResponse: {}", issue.getId(), e.getMessage());
                }
            }
            
            // Sort issues by reportedAt DESC (newest first)
            simpleIssues.sort((i1, i2) -> {
                if (i1.reportedAt() == null && i2.reportedAt() == null) return 0;
                if (i1.reportedAt() == null) return 1;
                if (i2.reportedAt() == null) return -1;
                return i2.reportedAt().compareTo(i1.reportedAt());
            });
            
            return simpleIssues;
        } catch (Exception e) {
            log.warn("Could not fetch issues for vehicle assignment {}: {}", vehicleAssignmentId, e.getMessage());
            return Collections.emptyList();
        }
    }

    private Map<UUID, List<SimpleIssueResponse>> buildIssuesMap(List<GetIssueImageResponse> issueImageResponses) {
        Map<UUID, List<SimpleIssueResponse>> map = new HashMap<>();
        if (issueImageResponses == null) return map;

        for (GetIssueImageResponse resp : issueImageResponses) {
            if (resp == null || resp.issue() == null) continue;
            try {
                var issue = resp.issue();
                if (issue.vehicleAssignmentEntity() == null || issue.vehicleAssignmentEntity().id() == null) continue;
                UUID vehicleAssignmentId = UUID.fromString(issue.vehicleAssignmentEntity().id().toString());
                SimpleIssueResponse simple = safeToSimpleIssueResponse(resp);
                if (simple != null) {
                    map.computeIfAbsent(vehicleAssignmentId, k -> new ArrayList<>()).add(simple);
                }
            } catch (Exception ignored) {
            }
        }
        
        // Sort issues by reportedAt DESC (newest first) for each vehicle assignment
        map.values().forEach(issues -> issues.sort((i1, i2) -> {
            if (i1.reportedAt() == null && i2.reportedAt() == null) return 0;
            if (i1.reportedAt() == null) return 1;
            if (i2.reportedAt() == null) return -1;
            return i2.reportedAt().compareTo(i1.reportedAt());
        }));
        
        return map;
    }

    private SimpleIssueResponse safeToSimpleIssueResponse(GetIssueImageResponse response) {
        if (response == null || response.issue() == null) return null;

        var issue = response.issue();

        SimpleStaffResponse staffResponse = null;
        if (issue.staff() != null) {
            var s = issue.staff();
            staffResponse = new SimpleStaffResponse(
                    s.getId(),
                    s.getFullName(),
                    s.getPhoneNumber()
            );
        }

        String vehicleAssignmentIdStr = null;
        if (issue.vehicleAssignmentEntity() != null && issue.vehicleAssignmentEntity().id() != null) {
            vehicleAssignmentIdStr = issue.vehicleAssignmentEntity().id().toString();
        }

        String issueTypeName = issue.issueTypeEntity() != null ? issue.issueTypeEntity().issueTypeName() : null;
        String issueTypeDescription = issue.issueTypeEntity() != null ? issue.issueTypeEntity().description() : null;
        var issueCategory = issue.issueCategory();

        // Issue images
        List<String> issueImages = response.imageUrl() != null ? new ArrayList<>(response.imageUrl()) : Collections.emptyList();

        // Note: Transactions will be fetched in service layer to avoid circular dependency
        // Mappers should not call other services - violation of separation of concerns

        return new SimpleIssueResponse(
                issue.id() != null ? issue.id().toString() : null,
                issue.description(),
                issue.locationLatitude(),
                issue.locationLongitude(),
                issue.status(),
                vehicleAssignmentIdStr,
                staffResponse,
                issueTypeName,
                issueTypeDescription,
                issue.reportedAt(),
                issueCategory,
                issueImages, // Issue images moved inside
                // SEAL_REPLACEMENT fields
                issue.oldSeal(),
                issue.newSeal(),
                issue.sealRemovalImage(),
                issue.newSealAttachedImage(),
                issue.newSealConfirmedAt(),
                // ORDER_REJECTION fields
                issue.paymentDeadline(),
                issue.calculatedFee(),
                issue.adjustedFee(),
                issue.finalFee(),
                issue.affectedOrderDetails(),
                issue.returnTransaction(), // Refund
                null, // Transaction (deprecated)
                null // Transactions list (will be fetched in service layer)
        );
    }

    /**
     * Calculate deposit amount based on adjusted value (if available) or total price and deposit percent from contract settings
     * Priority: adjustedValue > totalPrice
     * @param totalPrice The total price of the order
     * @param adjustedValue The adjusted value from contract (optional)
     * @return The calculated deposit amount, or null if total price is null
     */
    private BigDecimal calculateDepositAmount(BigDecimal totalPrice, BigDecimal adjustedValue) {
        // Use adjustedValue if available, otherwise use totalPrice
        BigDecimal baseAmount = adjustedValue != null && adjustedValue.compareTo(BigDecimal.ZERO) > 0 
            ? adjustedValue 
            : totalPrice;

        if (baseAmount == null) {
            return null;
        }

        try {
            // Get the latest contract setting
            var contractSetting = contractSettingEntityService.findFirstByOrderByCreatedAtAsc().orElse(null);
            BigDecimal depositPercent = contractSetting != null && contractSetting.getDepositPercent() != null 
                    ? contractSetting.getDepositPercent() 
                    : new BigDecimal("30"); // Default to 30%
            
            // Use unified pricing for consistent rounding across all systems
            return PricingUtils.calculateRoundedDeposit(baseAmount, depositPercent);
        } catch (Exception e) {
            log.warn("Error calculating deposit amount: {}", e.getMessage());
            // Default to 30% on error using unified pricing
            return PricingUtils.calculateRoundedDeposit(baseAmount, new BigDecimal("30"));
        }
    }
    
    /**
     * Get device info from vehicle assignment's device_ids field
     */
    private List<StaffVehicleAssignmentFullResponse.DeviceInfo> getDeviceInfoFromAssignment(VehicleAssignmentEntity entity) {
        if (entity == null || entity.getDeviceIds() == null || entity.getDeviceIds().trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        try {
            // Parse comma-separated device IDs
            String[] deviceIdStrings = entity.getDeviceIds().split(",");
            List<StaffVehicleAssignmentFullResponse.DeviceInfo> deviceInfoList = new ArrayList<>();
            
            for (String deviceIdStr : deviceIdStrings) {
                try {
                    UUID deviceId = UUID.fromString(deviceIdStr.trim());
                    deviceEntityService.findEntityById(deviceId).ifPresent(device -> {
                        String deviceTypeName = device.getDeviceTypeEntity() != null 
                                ? device.getDeviceTypeEntity().getDeviceTypeName() 
                                : null;
                        
                        deviceInfoList.add(new StaffVehicleAssignmentFullResponse.DeviceInfo(
                                device.getId(),
                                device.getDeviceCode(),
                                device.getManufacturer(),
                                device.getModel(),
                                device.getIpAddress(),
                                device.getFirmwareVersion(),
                                deviceTypeName
                        ));
                    });
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid device ID format in assignment {}: {}", entity.getId(), deviceIdStr);
                }
            }
            
            return deviceInfoList;
        } catch (Exception e) {
            log.error("Error getting device info for assignment {}: {}", entity.getId(), e.getMessage());
            return Collections.emptyList();
        }
    }
}
