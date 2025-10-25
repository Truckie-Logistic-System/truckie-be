package capstone_project.service.mapper.order;

import capstone_project.dtos.response.issue.SimpleIssueImageResponse;
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

        Map<UUID, SimpleIssueImageResponse> issuesByVehicleAssignment = buildIssuesMap(issueImageResponses);

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
                        // Get issue for this vehicle assignment
                        SimpleIssueImageResponse issue = issuesByVehicleAssignment.get(vaId);
                        List<String> photoCompletions = getPhotoCompletionsFor(photoCompletionResponses, vaId);
                        return toSimpleVehicleAssignmentResponse(vaResponse, issue, photoCompletions, vehicleCache, userCache);
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
                response.category().categoryName(),
                simpleOrderDetails,
                vehicleAssignments  // Add aggregated vehicle assignments
        );
    }

    private SimpleVehicleAssignmentResponse toSimpleVehicleAssignmentResponse(
            capstone_project.dtos.response.vehicle.VehicleAssignmentResponse vaResponse,
            SimpleIssueImageResponse issue,
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
                        vehicleEntity.getVehicleTypeEntity() != null ? vehicleEntity.getVehicleTypeEntity().getVehicleTypeName() : null
                );
            }
        } catch (Exception ignored) {
        }

        // Get primary driver info
        try {
            UUID d1 = vaResponse.driver_id_1();
            var d = getUserFromCache(d1, userCache);
            if (d != null) primaryDriver = new SimpleDriverResponse(d.getId().toString(), d.getFullName(), d.getPhoneNumber());
        } catch (Exception ignored) {
        }

        // Get secondary driver info
        try {
            UUID d2 = vaResponse.driver_id_2();
            var d = getUserFromCache(d2, userCache);
            if (d != null) secondaryDriver = new SimpleDriverResponse(d.getId().toString(), d.getFullName(), d.getPhoneNumber());
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
        List<GetSealResponse> orderSeals = Collections.emptyList();
        try {
            UUID vehicleAssignmentId = vaResponse.id();
            List<GetSealResponse> raw = sealService.getAllSealsByVehicleAssignmentId(vehicleAssignmentId);
            if (raw != null) orderSeals = raw;
        } catch (Exception e) {
            // Keep orderSeals as empty list
        }

        // Ensure photoCompletions and issue lists are non-null
        List<String> safePhotoCompletions = photoCompletions != null ? photoCompletions : Collections.emptyList();
        List<SimpleIssueImageResponse> issuesList = issue != null ? List.of(issue) : Collections.emptyList();

        // Build vehicleAssignment response
        return new SimpleVehicleAssignmentResponse(
                vaResponse.id().toString(),
                vehicle,
                primaryDriver,
                secondaryDriver,
                vaResponse.status(),
                vaResponse.trackingCode(),
                issuesList,
                safePhotoCompletions,
                orderSeals,
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
                response.issue().vehicleAssignmentEntity().id().toString(),
                staffResponse,
                response.issue().issueTypeEntity().issueTypeName()
        );

        return new SimpleIssueImageResponse(simpleIssue, response.imageUrl());
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

    private Map<UUID, SimpleIssueImageResponse> buildIssuesMap(List<GetIssueImageResponse> issueImageResponses) {
        Map<UUID, SimpleIssueImageResponse> map = new HashMap<>();
        if (issueImageResponses == null) return map;

        for (GetIssueImageResponse resp : issueImageResponses) {
            if (resp == null || resp.issue() == null) continue;
            try {
                // guard nested nulls
                var issue = resp.issue();
                if (issue.vehicleAssignmentEntity() == null || issue.vehicleAssignmentEntity().id() == null) continue;
                UUID vehicleAssignmentId = UUID.fromString(issue.vehicleAssignmentEntity().id().toString());
                SimpleIssueImageResponse simple = safeToSimpleIssueImageResponse(resp);
                if (simple != null) map.put(vehicleAssignmentId, simple);
            } catch (Exception ignored) {
                // ignore invalid UUIDs or mapping errors
            }
        }
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


    private SimpleIssueImageResponse safeToSimpleIssueImageResponse(GetIssueImageResponse response) {
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

        SimpleIssueResponse simpleIssue = new SimpleIssueResponse(
                issue.id() != null ? issue.id().toString() : null,
                issue.description(),
                issue.locationLatitude(),
                issue.locationLongitude(),
                issue.status(),
                vehicleAssignmentIdStr,
                staffResponse,
                issueTypeName
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
