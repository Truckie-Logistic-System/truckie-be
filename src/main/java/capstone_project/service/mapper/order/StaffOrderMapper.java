package capstone_project.service.mapper.order;

import capstone_project.dtos.response.issue.SimpleIssueImageResponse;
import capstone_project.dtos.response.order.*;
import capstone_project.dtos.response.order.contract.ContractResponse;
import capstone_project.dtos.response.order.contract.SimpleContractResponse;
import capstone_project.dtos.response.order.seal.GetOrderSealResponse;
import capstone_project.dtos.response.order.transaction.SimpleTransactionResponse;
import capstone_project.dtos.response.order.transaction.TransactionResponse;
import capstone_project.dtos.response.vehicle.VehicleAssignmentResponse;
import capstone_project.entity.auth.UserEntity;
import capstone_project.entity.device.CameraTrackingEntity;
import capstone_project.entity.user.driver.DriverEntity;
import capstone_project.entity.user.driver.PenaltyHistoryEntity;
import capstone_project.repository.entityServices.auth.UserEntityService;
import capstone_project.repository.entityServices.device.CameraTrackingEntityService;
import capstone_project.repository.entityServices.order.VehicleFuelConsumptionEntityService;
import capstone_project.repository.entityServices.user.DriverEntityService;
import capstone_project.repository.entityServices.user.PenaltyHistoryEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleEntityService;
import capstone_project.service.services.order.seal.OrderSealService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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

    public StaffOrderForStaffResponse toStaffOrderForStaffResponse(
            GetOrderResponse orderResponse,
            ContractResponse contractResponse,
            List<TransactionResponse> transactionResponses
    ) {
        // Convert Order with enhanced information for staff
        StaffOrderResponse staffOrderResponse = toStaffOrderResponseWithEnhancedInfo(orderResponse);

        // Convert Contract
        SimpleContractResponse simpleContractResponse = contractResponse != null ?
                toSimpleContractResponse(contractResponse) : null;

        // Convert Transactions
        List<SimpleTransactionResponse> simpleTransactionResponses = transactionResponses.stream()
                .map(this::toSimpleTransactionResponse)
                .collect(Collectors.toList());

        return new StaffOrderForStaffResponse(
                staffOrderResponse,
                simpleContractResponse,
                simpleTransactionResponses
        );
    }

    private StaffOrderResponse toStaffOrderResponseWithEnhancedInfo(GetOrderResponse response) {
        // Combine addresses like in SimpleOrderMapper
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
                response.totalPrice(),
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
        // Create order size information
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

        // Create enhanced vehicle assignment for staff
        StaffVehicleAssignmentResponse vehicleAssignment = null;
        if (detail.vehicleAssignmentId() != null) {
            vehicleAssignment = toStaffVehicleAssignmentResponse(detail.vehicleAssignmentId());
        }

        // GetOrderDetailResponse doesn't have issue and photoCompletions
        SimpleIssueImageResponse issue = null;
        List<String> photoCompletions = new ArrayList<>();

        return new StaffOrderDetailResponse(
                detail.trackingCode(), // Use trackingCode as identifier
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

        // Get additional information for staff
        List<PenaltyHistoryResponse> penalties = getPenaltiesByVehicleAssignmentId(vehicleAssignmentId,
                                                 vehicleAssignmentResponse.driver_id_1(),
                                                 vehicleAssignmentResponse.driver_id_2());

        List<CameraTrackingResponse> cameraTrackings = getCameraTrackingsByVehicleAssignmentId(vehicleAssignmentId);
        VehicleFuelConsumptionResponse fuelConsumption = getFuelConsumptionByVehicleAssignmentId(vehicleAssignmentId);

        // Get issues for this vehicle assignment
        List<SimpleIssueImageResponse> issues = getIssuesByVehicleAssignmentId(vehicleAssignmentId);

        // Create enhanced driver responses with complete information from driver and user tables
        StaffDriverResponse primaryDriver = null;
        StaffDriverResponse secondaryDriver = null;

        try {
            if (vehicleAssignmentResponse.driver_id_1() != null) {
                primaryDriver = getEnhancedDriverInfo(vehicleAssignmentResponse.driver_id_1());
            }

            if (vehicleAssignmentResponse.driver_id_2() != null) {
                secondaryDriver = getEnhancedDriverInfo(vehicleAssignmentResponse.driver_id_2());
            }
        } catch (Exception e) {
            log.warn("Could not fetch driver info for vehicle assignment {}: {}", vehicleAssignmentId, e.getMessage());
        }

        // Create enhanced vehicle response from vehicleId
        StaffVehicleResponse vehicle = null;
        try {
            if (vehicleAssignmentResponse.vehicleId() != null) {
                var vehicleEntity = vehicleEntityService.findEntityById(vehicleAssignmentResponse.vehicleId()).orElse(null);
                if (vehicleEntity != null) {
                    vehicle = new StaffVehicleResponse(
                            null, // Remove ID as this is already part of the vehicle assignment
                            vehicleEntity.getManufacturer(),
                            vehicleEntity.getModel(),
                            vehicleEntity.getLicensePlateNumber(),
                            vehicleEntity.getVehicleTypeEntity() != null ? vehicleEntity.getVehicleTypeEntity().getVehicleTypeName() : null
                    );
                }
            }
        } catch (Exception e) {
            log.warn("Could not fetch vehicle info for vehicle assignment {}: {}", vehicleAssignmentId, e.getMessage());
        }

        // Get order seals
        List<GetOrderSealResponse> orderSeals = new ArrayList<>();
        try {
            orderSeals = orderSealService.getAllOrderSealsByVehicleAssignmentId(vehicleAssignmentId);
        } catch (Exception e) {
            log.warn("Could not fetch order seals for vehicle assignment {}: {}", vehicleAssignmentId, e.getMessage());
        }

        return new StaffVehicleAssignmentResponse(
                vehicleAssignmentId,
                vehicle,
                primaryDriver,
                secondaryDriver,
                vehicleAssignmentResponse.status(),
                penalties,
                cameraTrackings,
                fuelConsumption,
                orderSeals,
                new ArrayList<>(), // journeyHistory
                issues // Added issues to the constructor
        );
    }

    private StaffDriverResponse getEnhancedDriverInfo(UUID driverId) {
        if (driverId == null) return null;

        try {
            // Get user information
            UserEntity userEntity = userEntityService.findEntityById(driverId).orElse(null);
            if (userEntity == null) return null;

            // Get driver information from driver entity
            DriverEntity driverEntity = driverEntityService.findByUserId(driverId).orElse(null);

            // Create enhanced driver response with combined information
            return new StaffDriverResponse(
                    null, // Remove ID as this is already part of the vehicle assignment
                    userEntity.getFullName(),
                    userEntity.getPhoneNumber(),
                    userEntity.getEmail(),
                    userEntity.getImageUrl(), // Added image URL from user entity
                    userEntity.getGender(), // Added gender from user entity
                    userEntity.getDateOfBirth(), // Added date of birth from user entity
                    driverEntity != null ? driverEntity.getIdentityNumber() : null, // Added identity number
                    driverEntity != null ? driverEntity.getDriverLicenseNumber() : null,
                    driverEntity != null ? driverEntity.getCardSerialNumber() : null, // Added card serial number
                    driverEntity != null ? driverEntity.getPlaceOfIssue() : null, // Added place of issue
                    driverEntity != null ? driverEntity.getDateOfIssue() : null, // Added date of issue
                    driverEntity != null ? driverEntity.getDateOfExpiry() : null,
                    driverEntity != null ? driverEntity.getLicenseClass() : null, // Added license class
                    driverEntity != null ? driverEntity.getDateOfPassing() : null, // Added date of passing
                    userEntity.getStatus(),
                    null, // Address information not available in provided entities
                    userEntity.getCreatedAt()
            );
        } catch (Exception e) {
            log.warn("Could not fetch enhanced driver info for driver ID {}: {}", driverId, e.getMessage());
            return null;
        }
    }

    private List<PenaltyHistoryResponse> getPenaltiesByVehicleAssignmentId(UUID vehicleAssignmentId, UUID primaryDriverId, UUID secondaryDriverId) {
        if (vehicleAssignmentId == null) return new ArrayList<>();

        try {
            List<PenaltyHistoryEntity> penalties = penaltyHistoryEntityService.findByVehicleAssignmentId(vehicleAssignmentId);
            return penalties.stream()
                    .map(entity -> {
                        // Get the complete driver information for the penalty
                        StaffDriverResponse driverInfo = null;

                        // Try to get the driver who issued this penalty
                        if (entity.getIssueBy() != null) {
                            driverInfo = getEnhancedDriverInfo(entity.getIssueBy().getId());
                        }
                        // If no specific driver is linked to the penalty, default to the primary driver
                        else if (primaryDriverId != null) {
                            driverInfo = getEnhancedDriverInfo(primaryDriverId);
                        }

                        // Create the penalty response with full driver information
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
                                driverInfo  // Including the complete driver object instead of just an ID
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
                            null, // Remove vehicle assignment ID as this is already part of the containing object
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
        if (vehicleAssignmentId == null) return new ArrayList<>();

        try {
            // Fetch issues from the IssueEntityService by vehicle assignment ID
            // For now, we'll return an empty list since the current services don't support
            // retrieving multiple issues for a vehicle assignment

            // In a real implementation, you would:
            // 1. Inject an appropriate repository or service that can fetch all issues for a vehicle assignment
            // 2. Transform those issues into SimpleIssueImageResponse objects
            // 3. Return the list of transformed objects

            log.info("Fetching issues for vehicle assignment ID: {}", vehicleAssignmentId);
            return new ArrayList<>();
        } catch (Exception e) {
            log.warn("Could not fetch issues for vehicle assignment {}: {}", vehicleAssignmentId, e.getMessage());
            return new ArrayList<>();
        }
    }
}
