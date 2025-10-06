package capstone_project.service.mapper.order;

import capstone_project.dtos.response.issue.GetIssueImageResponse;
import capstone_project.dtos.response.issue.SimpleIssueImageResponse;
import capstone_project.dtos.response.issue.SimpleIssueResponse;
import capstone_project.dtos.response.issue.SimpleStaffResponse;
import capstone_project.dtos.response.order.*;
import capstone_project.dtos.response.order.contract.ContractResponse;
import capstone_project.dtos.response.order.contract.SimpleContractResponse;
import capstone_project.dtos.response.order.seal.GetOrderSealResponse;
import capstone_project.dtos.response.order.transaction.SimpleTransactionResponse;
import capstone_project.dtos.response.order.transaction.TransactionResponse;
import capstone_project.dtos.response.order.PhotoCompletionResponse;
import capstone_project.dtos.response.vehicle.VehicleAssignmentResponse;
import capstone_project.entity.auth.UserEntity;
import capstone_project.entity.device.CameraTrackingEntity;
import capstone_project.entity.user.driver.DriverEntity;
import capstone_project.entity.user.driver.PenaltyHistoryEntity;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.repository.entityServices.auth.UserEntityService;
import capstone_project.repository.entityServices.device.CameraTrackingEntityService;
import capstone_project.repository.entityServices.order.VehicleFuelConsumptionEntityService;
import capstone_project.repository.entityServices.user.DriverEntityService;
import capstone_project.repository.entityServices.user.PenaltyHistoryEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleAssignmentEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleEntityService;
import capstone_project.service.services.issue.IssueImageService;
import capstone_project.service.services.order.order.JourneyHistoryService;
import capstone_project.service.services.order.seal.OrderSealService;
import capstone_project.service.services.order.order.PhotoCompletionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class StaffOrderMapper {

    private final PenaltyHistoryEntityService penaltyHistoryEntityService;
    private final CameraTrackingEntityService cameraTrackingEntityService;
    private final VehicleFuelConsumptionEntityService vehicleFuelConsumptionEntityService;
    private final UserEntityService userEntityService;
    private final DriverEntityService driverEntityService;
    private final VehicleEntityService vehicleEntityService;
    private final OrderSealService orderSealService;
    private final VehicleAssignmentEntityService vehicleAssignmentEntityService;
    private final JourneyHistoryService journeyHistoryService;
    private final IssueImageService issueImageService;
    // PhotoCompletionService may not exist in every project; if not present remove this field and helper below
    private final PhotoCompletionService photoCompletionService;

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
                    effectiveTotal = contractResponse.supportedValue();
                }
            }
        } catch (Exception ignored) {
        }
        if (effectiveTotal == null && orderResponse != null) {
            effectiveTotal = orderResponse.totalPrice();
        }

        // Convert Order with enhanced information for staff, passing effective total
        StaffOrderResponse staffOrderResponse = toStaffOrderResponseWithEnhancedInfo(orderResponse, effectiveTotal);

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

    private StaffOrderResponse toStaffOrderResponseWithEnhancedInfo(GetOrderResponse response, BigDecimal effectiveTotal) {
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

        // Process order details with enhanced information for staff
        List<StaffOrderDetailResponse> staffOrderDetails = response.orderDetails().stream()
                .map(this::toStaffOrderDetailResponseWithEnhancedInfo)
                .collect(Collectors.toList());

        return new StaffOrderResponse(
                response.id(),
                effectiveTotal,
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
                response.category().categoryName(),
                staffOrderDetails
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

        StaffVehicleAssignmentResponse vehicleAssignment = null;
        if (detail.vehicleAssignmentId() != null) {
            vehicleAssignment = toStaffVehicleAssignmentResponse(detail.vehicleAssignmentId());
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
                vehicleAssignment
        );
    }

    private StaffVehicleAssignmentResponse toStaffVehicleAssignmentResponse(VehicleAssignmentResponse vehicleAssignmentResponse) {
        UUID vehicleAssignmentId = vehicleAssignmentResponse.id();

        log.info("Processing vehicle assignment with ID: {}", vehicleAssignmentId);

        VehicleAssignmentEntity vehicleAssignmentEntity = null;
        try {
            vehicleAssignmentEntity = vehicleAssignmentEntityService.findEntityById(vehicleAssignmentId).orElse(null);
            if (vehicleAssignmentEntity == null) {
                log.warn("Vehicle assignment entity not found in database for ID: {}", vehicleAssignmentId);
            }
        } catch (Exception e) {
            log.error("Error retrieving vehicle assignment entity from database: {}", e.getMessage(), e);
        }

        List<PenaltyHistoryResponse> penalties = getPenaltiesByVehicleAssignmentId(vehicleAssignmentId,
                vehicleAssignmentEntity != null && vehicleAssignmentEntity.getDriver1() != null ?
                        vehicleAssignmentEntity.getDriver1().getUser().getId() : vehicleAssignmentResponse.driver_id_1(),
                vehicleAssignmentEntity != null && vehicleAssignmentEntity.getDriver2() != null ?
                        vehicleAssignmentEntity.getDriver2().getUser().getId() : vehicleAssignmentResponse.driver_id_2());

        List<CameraTrackingResponse> cameraTrackings = getCameraTrackingsByVehicleAssignmentId(vehicleAssignmentId);
        VehicleFuelConsumptionResponse fuelConsumption = getFuelConsumptionByVehicleAssignmentId(vehicleAssignmentId);
        List<String> photoCompletions = getPhotoCompletionsByVehicleAssignmentId(vehicleAssignmentId);
        // Get issues for this vehicle assignment (null-safe, full lookup)
        List<SimpleIssueImageResponse> issues = getIssuesByVehicleAssignmentId(vehicleAssignmentId);
        if (issues == null) issues = Collections.emptyList();

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
                            vehicleEntity.getVehicleTypeEntity() != null ? vehicleEntity.getVehicleTypeEntity().getVehicleTypeName() : null
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
        List<GetOrderSealResponse> orderSeals = Collections.emptyList();
        try {
            List<GetOrderSealResponse> raw = orderSealService.getAllOrderSealsByVehicleAssignmentId(vehicleAssignmentId);
            if (raw != null) orderSeals = raw;
        } catch (Exception e) {
            log.warn("Could not fetch order seals for {}: {}", vehicleAssignmentId, e.getMessage());
        }

        // Ensure lists are non-null
        if (orderSeals == null) orderSeals = Collections.emptyList();
        if (journeyHistories == null) journeyHistories = Collections.emptyList();
        if (issues == null) issues = Collections.emptyList();
        if (photoCompletions == null) photoCompletions = Collections.emptyList();

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
                cameraTrackings,
                fuelConsumption,
                orderSeals,
                journeyHistories,
                photoCompletions,
                issues
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

    private List<PenaltyHistoryResponse> getPenaltiesByVehicleAssignmentId(UUID vehicleAssignmentId, UUID primaryDriverId, UUID secondaryDriverId) {
        if (vehicleAssignmentId == null) return new ArrayList<>();

        try {
            List<PenaltyHistoryEntity> penalties = penaltyHistoryEntityService.findByVehicleAssignmentId(vehicleAssignmentId);
            return penalties.stream()
                    .map(entity -> {
                        StaffDriverResponse driverInfo = null;
                        if (entity.getIssueBy() != null) {
                            driverInfo = getEnhancedDriverInfo(entity.getIssueBy().getId());
                        } else if (primaryDriverId != null) {
                            driverInfo = getEnhancedDriverInfo(primaryDriverId);
                        }

                        return new PenaltyHistoryResponse(
                                entity.getId(),
                                entity.getViolationType(),
                                entity.getViolationDescription(),
                                entity.getPenaltyAmount(),
                                entity.getPenaltyDate(),
                                entity.getLocation(),
                                entity.getStatus(),
                                entity.getPaymentDate(),
                                entity.getDisputeReason(),
                                driverInfo
                        );
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Could not fetch penalties for vehicle assignment {}: {}", vehicleAssignmentId, e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<CameraTrackingResponse> getCameraTrackingsByVehicleAssignmentId(UUID vehicleAssignmentId) {
        if (vehicleAssignmentId == null) return new ArrayList<>();

        try {
            List<CameraTrackingEntity> cameraTrackings = cameraTrackingEntityService.findByVehicleAssignmentId(vehicleAssignmentId);
            return cameraTrackings.stream()
                    .map(entity -> new CameraTrackingResponse(
                            entity.getId(),
                            entity.getVideoUrl(),
                            entity.getTrackingAt(),
                            entity.getStatus(),
                            null,
                            entity.getDeviceEntity() != null ?
                                    entity.getDeviceEntity().getDeviceCode() + " " + entity.getDeviceEntity().getModel() : null
                    ))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Could not fetch camera trackings for vehicle assignment {}: {}", vehicleAssignmentId, e.getMessage());
            return new ArrayList<>();
        }
    }

    private VehicleFuelConsumptionResponse getFuelConsumptionByVehicleAssignmentId(UUID vehicleAssignmentId) {
        if (vehicleAssignmentId == null) return null;

        try {
            return vehicleFuelConsumptionEntityService.findByVehicleAssignmentId(vehicleAssignmentId)
                    .map(entity -> new VehicleFuelConsumptionResponse(
                            entity.getId(),
                            entity.getOdometerReadingAtRefuel(),
                            entity.getOdometerAtStartUrl(),
                            entity.getOdometerAtFinishUrl(),
                            entity.getOdometerAtEndUrl(),
                            entity.getDateRecorded(),
                            entity.getNotes(),
                            entity.getFuelTypeEntity() != null ? entity.getFuelTypeEntity().getName() : null,
                            entity.getFuelTypeEntity() != null ? entity.getFuelTypeEntity().getDescription() : null
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
                contract.supportedValue(),
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
                transaction.orderCode(),
                transaction.amount(),
                transaction.currencyCode(),
                transaction.status(),
                transaction.paymentDate()
        );
    }

    private String combineAddress(String street, String ward, String province) {
        StringBuilder address = new StringBuilder();
        if (street != null && !street.isEmpty()) {
            address.append(street);
        }
        if (ward != null && !ward.isEmpty()) {
            if (address.length() > 0) address.append(", ");
            address.append(ward);
        }
        if (province != null && !province.isEmpty()) {
            if (address.length() > 0) address.append(", ");
            address.append(province);
        }
        return address.toString();
    }

    private List<SimpleIssueImageResponse> getIssuesByVehicleAssignmentId(UUID vehicleAssignmentId) {
        if (vehicleAssignmentId == null) return Collections.emptyList();

        try {
            // IssueImageService returns a GetIssueImageResponse for a vehicle assignment
            GetIssueImageResponse resp = issueImageService.getByVehicleAssignment(vehicleAssignmentId);
            if (resp == null || resp.issue() == null) {
                return Collections.emptyList();
            }
            SimpleIssueImageResponse simple = toSimpleIssueImageResponse(resp);
            return simple != null ? List.of(simple) : Collections.emptyList();
        } catch (Exception e) {
            log.warn("Could not fetch issues for vehicle assignment {}: {}", vehicleAssignmentId, e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<String> getPhotoCompletionsByVehicleAssignmentId(UUID vehicleAssignmentId) {
        if (vehicleAssignmentId == null) return Collections.emptyList();

        try {
            if (photoCompletionService == null) return Collections.emptyList();
            List<PhotoCompletionResponse> raw = photoCompletionService.getByVehicleAssignmentId(vehicleAssignmentId);
            if (raw == null) return Collections.emptyList();
            return raw.stream()
                    .map(PhotoCompletionResponse::imageUrl)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Could not fetch photo completions for {}: {}", vehicleAssignmentId, e.getMessage());
            return Collections.emptyList();
        }
    }

    private SimpleIssueImageResponse toSimpleIssueImageResponse(GetIssueImageResponse response) {
        if (response == null || response.issue() == null) {
            return null;
        }

        SimpleStaffResponse staffResponse = null;
        if (response.issue().staff() != null) {
            staffResponse = new SimpleStaffResponse(
                    response.issue().staff().getId(),
                    response.issue().staff().getFullName(),
                    response.issue().staff().getPhoneNumber()
            );
        }

        SimpleIssueResponse simpleIssue = new SimpleIssueResponse(
                response.issue().id().toString(),
                response.issue().description(),
                response.issue().locationLatitude(),
                response.issue().locationLongitude(),
                response.issue().status(),
                response.issue().vehicleAssignmentEntity() != null && response.issue().vehicleAssignmentEntity().id() != null ?
                        response.issue().vehicleAssignmentEntity().id().toString() : null,
                staffResponse,
                response.issue().issueTypeEntity() != null ? response.issue().issueTypeEntity().issueTypeName() : null
        );

        List<String> images = response.imageUrl() != null ? new ArrayList<>(response.imageUrl()) : Collections.emptyList();
        return new SimpleIssueImageResponse(simpleIssue, images);
    }

    /**
     * Calculate the total distance by summing up distances from all journey segments
     * @param segments The list of journey segments
     * @return The total distance as a Double, or 0.0 if segments are null or empty
     */
    private Double calculateTotalDistance(List<JourneySegmentResponse> segments) {
        if (segments == null || segments.isEmpty()) {
            return 0.0;
        }

        return segments.stream()
                .mapToDouble(segment -> segment.distanceMeters() != null ? segment.distanceMeters() : 0.0)
                .sum();
    }
}
