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
import capstone_project.repository.entityServices.setting.ContractSettingEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleAssignmentEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleEntityService;
import capstone_project.service.services.order.order.JourneyHistoryService;
import capstone_project.service.services.order.seal.SealService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class SimpleOrderMapper {
    private final UserEntityService userEntityService;
    private final VehicleEntityService vehicleEntityService;
    private final SealService sealService;
    private final JourneyHistoryService journeyHistoryService;
    private final VehicleAssignmentEntityService vehicleAssignmentEntityService;
    private final ContractSettingEntityService contractSettingEntityService;

    public SimpleOrderForCustomerResponse toSimpleOrderForCustomerResponse(
            GetOrderResponse orderResponse,
            List<GetIssueImageResponse> issueImageResponses,
            Map<UUID, List<PhotoCompletionResponse>> photoCompletionResponses,
            ContractResponse contractResponse,
            List<TransactionResponse> transactionResponses
    ) {
        // Convert Contract
        SimpleContractResponse simpleContractResponse = contractResponse != null ?
                toSimpleContractResponse(contractResponse) : null;

        // Convert Transactions
        List<SimpleTransactionResponse> simpleTransactionResponses = transactionResponses.stream()
                .map(this::toSimpleTransactionResponse)
                .collect(Collectors.toList());

        BigDecimal effectiveTotal = null;
        try {
            if (contractResponse != null) {
                BigDecimal contractTotal = contractResponse.totalValue();
                log.info("Contract total value is {}", contractTotal);
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

        // Convert Order with enhanced vehicle assignments
        SimpleOrderResponse simpleOrderResponse = toSimpleOrderResponseWithTripInfo(
                orderResponse,
                issueImageResponses,
                photoCompletionResponses,
                effectiveTotal,
                depositAmount
        );

        return new SimpleOrderForCustomerResponse(
                simpleOrderResponse,
                simpleContractResponse,
                simpleTransactionResponses
        );
    }

    private SimpleOrderResponse toSimpleOrderResponseWithTripInfo(
            GetOrderResponse response,
            List<GetIssueImageResponse> issueImageResponses,
            Map<UUID, List<PhotoCompletionResponse>> photoCompletionResponses,
            BigDecimal effectiveTotal,
            BigDecimal depositAmount
    ) {
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

        return new SimpleOrderResponse(
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
            if (raw != null) {
                // Filter journey histories for customer view
                journeyHistories = filterJourneyHistoriesForCustomer(raw);
            }
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
                seals,
                journeyHistories
        );
    }

    private SimpleOrderDetailResponse toSimpleOrderDetailResponseWithTripInfo_OLD(
            GetOrderDetailResponse detail,
            SimpleIssueImageResponse issue,
            List<String> photoCompletions,
            Map<UUID, capstone_project.entity.vehicle.VehicleEntity> vehicleCache,
            Map<UUID, UserEntity> userCache
    ) {
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

        // Get the vehicle assignment entity directly using the UUID
        UUID vehicleAssignmentId = detail.vehicleAssignmentId();

        return new SimpleOrderDetailResponse(
                detail.trackingCode(), // Using trackingCode as an identifier since there's no id field
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
                vehicleAssignmentId // Pass UUID directly instead of SimpleVehicleAssignmentResponse
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

        // Simply pass the UUID directly - don't try to call methods on it
        UUID vehicleAssignmentId = detail.vehicleAssignmentId();

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
                vehicleAssignmentId  // Pass the UUID directly
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
     * Filter journey histories for customer view
     * Only include segments related to pickup, delivery, or intermediate points between them
     * Excludes carrier-related segments that customers don't need to see
     */
    private List<JourneyHistoryResponse> filterJourneyHistoriesForCustomer(List<JourneyHistoryResponse> rawHistories) {
        if (rawHistories == null || rawHistories.isEmpty()) {
            return Collections.emptyList();
        }

        // Return all journey histories without filtering
        return rawHistories;
    }

    /**
     * Previously filtered journey segments to only include those related to pickup, delivery, or intermediate points
     * Now returns all journey segments without filtering
     */
    private List<JourneySegmentResponse> filterJourneySegmentsForCustomer(List<JourneySegmentResponse> segments) {
        if (segments == null || segments.isEmpty()) {
            return Collections.emptyList();
        }

        // Return all journey segments without filtering
        return segments;
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
            if (contractSetting == null || contractSetting.getDepositPercent() == null) {
                // Default to 30% if no setting found
                return baseAmount.multiply(new BigDecimal("0.30")).setScale(0, RoundingMode.HALF_UP);
            }

            // Calculate deposit amount: baseAmount * (depositPercent / 100)
            BigDecimal depositPercent = contractSetting.getDepositPercent();
            return baseAmount.multiply(depositPercent).divide(new BigDecimal("100"), 0, RoundingMode.HALF_UP);
        } catch (Exception e) {
            log.warn("Error calculating deposit amount: {}", e.getMessage());
            // Default to 30% on error
            return baseAmount.multiply(new BigDecimal("0.30")).setScale(0, RoundingMode.HALF_UP);
        }
    }
}
