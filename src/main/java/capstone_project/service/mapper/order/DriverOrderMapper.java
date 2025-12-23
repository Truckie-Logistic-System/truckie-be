package capstone_project.service.mapper.order;

import capstone_project.dtos.response.issue.SimpleIssueResponse;
import capstone_project.dtos.response.issue.SimpleStaffResponse;
import capstone_project.dtos.response.order.*;
import capstone_project.dtos.response.order.contract.SimpleContractResponse;
import capstone_project.dtos.response.order.seal.GetSealResponse;
import capstone_project.dtos.response.order.transaction.SimpleTransactionResponse;
import capstone_project.dtos.response.issue.GetIssueImageResponse;
import capstone_project.dtos.response.order.GetOrderDetailResponse;
import capstone_project.dtos.response.order.GetOrderResponse;
import capstone_project.dtos.response.order.PhotoCompletionResponse;
import capstone_project.dtos.response.order.contract.ContractResponse;
import capstone_project.dtos.response.order.transaction.TransactionResponse;
import capstone_project.entity.auth.UserEntity;
import capstone_project.repository.entityServices.auth.UserEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleAssignmentEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleEntityService;
import capstone_project.service.services.order.order.JourneyHistoryService;
import capstone_project.service.services.order.seal.SealService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DriverOrderMapper {
    private final UserEntityService userEntityService;
    private final VehicleEntityService vehicleEntityService;
    private final SealService sealService;
    private final JourneyHistoryService journeyHistoryService;
    private final VehicleAssignmentEntityService vehicleAssignmentEntityService;

    public OrderForDriverResponse toOrderForDriverResponse(
            GetOrderResponse orderResponse,
            List<GetIssueImageResponse> issueImageResponses,
            Map<UUID, List<PhotoCompletionResponse>> photoCompletionResponses
    ) {
        // Convert Order with enhanced vehicle assignments
        DriverOrderResponse driverOrderResponse = toDriverOrderResponseWithTripInfo(
                orderResponse,
                issueImageResponses,
                photoCompletionResponses
        );

        return new OrderForDriverResponse(
                driverOrderResponse
        );
    }

    private DriverOrderResponse toDriverOrderResponseWithTripInfo(
            GetOrderResponse response,
            List<GetIssueImageResponse> issueImageResponses,
            Map<UUID, List<PhotoCompletionResponse>> photoCompletionResponses
    ) {
        String deliveryAddress = combineAddress(
                response.deliveryAddress().street(),
                response.deliveryAddress().ward(),
                response.pickupAddress().province()
        );

        String pickupAddress = combineAddress(
                response.pickupAddress().street(),
                response.pickupAddress().ward(),
                response.pickupAddress().province()
        );

        Map<UUID, List<SimpleIssueResponse>> issuesByVehicleAssignment = buildIssuesMap(issueImageResponses);

        // Per-request caches to avoid duplicate DB calls inside mapper
        Map<UUID, capstone_project.entity.vehicle.VehicleEntity> vehicleCache = new HashMap<>();
        Map<UUID, UserEntity> userCache = new HashMap<>();

        // Collect unique vehicle assignment IDs from order details
        Set<UUID> uniqueVehicleAssignmentIds = response.orderDetails().stream()
                .map(GetOrderDetailResponse::vehicleAssignmentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Build full vehicle assignment responses for unique IDs
        List<SimpleVehicleAssignmentResponse> vehicleAssignments = uniqueVehicleAssignmentIds.stream()
                .map(vaId -> {
                    // Find the vehicleAssignmentResponse with this ID from response.vehicleAssignments()
                    var vaResponse = response.vehicleAssignments().stream()
                            .filter(va -> va.id().equals(vaId))
                            .findFirst()
                            .orElse(null);
                    if (vaResponse != null) {
                        // Get ALL issues for this vehicle assignment
                        List<SimpleIssueResponse> issues = issuesByVehicleAssignment.getOrDefault(vaId, Collections.emptyList());
                        List<String> photoCompletions = getPhotoCompletionsFor(photoCompletionResponses, vaId);
                        return toSimpleVehicleAssignmentResponse(vaResponse, issues, photoCompletions, vehicleCache, userCache);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // Process order details with only vehicle assignment ID reference
        List<SimpleOrderDetailResponse> simpleOrderDetails = response.orderDetails().stream()
                .map(this::toSimpleOrderDetailResponse)
                .collect(Collectors.toList());

        return new DriverOrderResponse(
                response.id(),
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
                simpleOrderDetails,
                vehicleAssignments  // Add aggregated vehicle assignments
        );
    }

    private SimpleVehicleAssignmentResponse toSimpleVehicleAssignmentResponse(
            capstone_project.dtos.response.vehicle.VehicleAssignmentResponse vaResponse,
            List<SimpleIssueResponse> issues,
            List<String> photoCompletions,
            Map<UUID, capstone_project.entity.vehicle.VehicleEntity> vehicleCache,
            Map<UUID, UserEntity> userCache
    ) {
        VehicleResponse vehicle = null;
        SimpleDriverResponse primaryDriver = null;
        SimpleDriverResponse secondaryDriver = null;

        // Get vehicle info
        try {
            UUID vehicleId = vaResponse.vehicleId();
            var vehicleEntity = getVehicleFromCache(vehicleId, vehicleCache);
            if (vehicleEntity != null) {
                vehicle = new VehicleResponse(
                        vehicleEntity.getId(),
                        vehicleEntity.getManufacturer(),
                        vehicleEntity.getModel(),
                        vehicleEntity.getLicensePlateNumber(),
                        vehicleEntity.getVehicleTypeEntity() != null ? vehicleEntity.getVehicleTypeEntity().getVehicleTypeName() : null,
                        vehicleEntity.getVehicleTypeEntity() != null ? vehicleEntity.getVehicleTypeEntity().getDescription() : null
                );
            }
        } catch (Exception ignored) {
        }

        // Get primary driver info from nested object (already populated by VehicleAssignmentMapper)
        try {
            var driver1 = vaResponse.driver1();
            if (driver1 != null) {
                primaryDriver = new SimpleDriverResponse(
                    driver1.id().toString(), 
                    driver1.fullName(), 
                    driver1.phoneNumber()
                );
            }
        } catch (Exception ignored) {
        }

        // Get secondary driver info from nested object (already populated by VehicleAssignmentMapper)
        try {
            var driver2 = vaResponse.driver2();
            if (driver2 != null) {
                secondaryDriver = new SimpleDriverResponse(
                    driver2.id().toString(), 
                    driver2.fullName(), 
                    driver2.phoneNumber()
                );
            }
        } catch (Exception ignored) {
        }

        // Retrieve journey history for the vehicle assignment (null-safe)
        List<JourneyHistoryResponse> journeyHistories = Collections.emptyList();
        try {
            UUID vehicleAssignmentId = vaResponse.id();
            List<JourneyHistoryResponse> raw = journeyHistoryService.getByVehicleAssignmentId(vehicleAssignmentId);
            if (raw != null) journeyHistories = raw;
        } catch (Exception e) {
            // Keep journeyHistories as empty list
        }

        // Get all order seals for this vehicle assignment (null-safe)
        List<GetSealResponse> seals = Collections.emptyList();
        try {
            UUID vehicleAssignmentId = vaResponse.id();
            List<GetSealResponse> raw = sealService.getAllSealsByVehicleAssignmentId(vehicleAssignmentId);
            if (raw != null) seals = raw;
        } catch (Exception e) {
            // Keep seals as empty list
        }

        // Ensure photoCompletions and issues lists are non-null
        List<String> safePhotoCompletions = photoCompletions != null ? photoCompletions : Collections.emptyList();
        List<SimpleIssueResponse> safeIssues = issues != null ? issues : Collections.emptyList();

        // Build vehicleAssignment response
        return new SimpleVehicleAssignmentResponse(
                vaResponse.id().toString(),
                vehicle,
                primaryDriver,
                secondaryDriver,
                vaResponse.status(),
                vaResponse.trackingCode(),
                safeIssues,
                safePhotoCompletions,
                seals,
                journeyHistories
        );
    }

    // helpers: memoized lookups
    private capstone_project.entity.vehicle.VehicleEntity getVehicleFromCache(UUID id, Map<UUID, capstone_project.entity.vehicle.VehicleEntity> cache) {
        if (id == null) return null;
        return cache.computeIfAbsent(id, k -> {
            try {
                return vehicleEntityService.findEntityById(k).orElse(null);
            } catch (Exception e) {
                return null;
            }
        });
    }

    private UserEntity getUserFromCache(UUID id, Map<UUID, UserEntity> cache) {
        if (id == null) return null;
        return cache.computeIfAbsent(id, k -> {
            try {
                return userEntityService.findEntityById(k).orElse(null);
            } catch (Exception e) {
                return null;
            }
        });
    }

    private SimpleOrderDetailResponse toSimpleOrderDetailResponse(GetOrderDetailResponse detail) {
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

        return new SimpleOrderDetailResponse(
                detail.trackingCode(),
                detail.weightBaseUnit(),
                detail.unit(),
                detail.description(),
                detail.status(),
                detail.estimatedStartTime(),
                detail.createdAt(),
                detail.trackingCode(),
                orderSize,
                detail.vehicleAssignmentId(),  // Only store ID reference
                detail.declaredValue()  // Add declared value
        );
    }

    private Map<String, List<String>> convertPhotoCompletions(Map<UUID, List<PhotoCompletionResponse>> photoCompletionMap) {
        Map<String, List<String>> result = new HashMap<>();

        if (photoCompletionMap != null) {
            for (Map.Entry<UUID, List<PhotoCompletionResponse>> entry : photoCompletionMap.entrySet()) {
                String vehicleAssignmentId = entry.getKey().toString();
                List<String> imageUrls = entry.getValue().stream()
                        .map(PhotoCompletionResponse::imageUrl)
                        .collect(Collectors.toList());

                result.put(vehicleAssignmentId, imageUrls);
            }
        }

        return result;
    }

    private SimpleContractResponse toSimpleContractResponse(ContractResponse contract) {
        if (contract == null) {
            return null;
        }

        String staffName = "";
        try {
            if (contract.staffId() != null) {
                UserEntity staff = userEntityService.findEntityById(UUID.fromString(contract.staffId())).orElse(null);
                if (staff != null) {
                    staffName = staff.getFullName();
                }
            }
        } catch (Exception e) {
            // Handle exception gracefully
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
        if (transaction == null) {
            return null;
        }

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
            if (address.length() > 0) address.append(", ");
            address.append(ward);
        }
        if (province != null && !province.isEmpty()) {
            if (address.length() > 0) address.append(", ");
            address.append(province);
        }
        return address.toString();
    }

    private Map<UUID, List<SimpleIssueResponse>> buildIssuesMap(List<GetIssueImageResponse> issueImageResponses) {
        Map<UUID, List<SimpleIssueResponse>> map = new HashMap<>();
        if (issueImageResponses == null) return map;

        for (GetIssueImageResponse resp : issueImageResponses) {
            if (resp == null || resp.issue() == null) continue;
            try {
                // guard nested nulls
                var issue = resp.issue();
                if (issue.vehicleAssignmentEntity() == null || issue.vehicleAssignmentEntity().id() == null) continue;
                UUID vehicleAssignmentId = UUID.fromString(issue.vehicleAssignmentEntity().id().toString());
                SimpleIssueResponse simple = safeToSimpleIssueResponse(resp);
                if (simple != null) {
                    // Add to list instead of replacing
                    map.computeIfAbsent(vehicleAssignmentId, k -> new ArrayList<>()).add(simple);
                }
            } catch (Exception ignored) {
                // ignore invalid UUIDs or mapping errors
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

    // java
    private List<String> getPhotoCompletionsFor(Map<UUID, List<PhotoCompletionResponse>> photoCompletionResponses, UUID vehicleAssignmentId) {
        if (photoCompletionResponses == null) return Collections.emptyList(); // defensive
        if (vehicleAssignmentId == null) return Collections.emptyList();
        List<PhotoCompletionResponse> raw = photoCompletionResponses.get(vehicleAssignmentId);
        if (raw == null) return Collections.emptyList();
        return raw.stream()
                .map(PhotoCompletionResponse::imageUrl)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }


    private SimpleIssueResponse safeToSimpleIssueResponse(GetIssueImageResponse response) {
        if (response == null || response.issue() == null) return null;

        var issue = response.issue();

        SimpleStaffResponse staffResponse = null;
        if (issue.staff() != null) {
            var s = issue.staff();
            staffResponse = new SimpleStaffResponse(
                    UUID.fromString(s.getId()),
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
                null // Transactions list (driver view - won't fetch transactions)
        );
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
                .mapToDouble(segment -> segment.distanceKilometers() != null ? segment.distanceKilometers().doubleValue() : 0.0)
                .sum();
    }
}
