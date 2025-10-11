package capstone_project.service.mapper.order;

import capstone_project.dtos.response.issue.SimpleIssueImageResponse;
import capstone_project.dtos.response.issue.SimpleIssueResponse;
import capstone_project.dtos.response.issue.SimpleStaffResponse;
import capstone_project.dtos.response.order.*;
import capstone_project.dtos.response.order.contract.SimpleContractResponse;
import capstone_project.dtos.response.order.seal.GetOrderSealResponse;
import capstone_project.dtos.response.order.transaction.SimpleTransactionResponse;
import capstone_project.dtos.response.issue.GetIssueImageResponse;
import capstone_project.dtos.response.order.GetOrderDetailResponse;
import capstone_project.dtos.response.order.GetOrderResponse;
import capstone_project.dtos.response.order.PhotoCompletionResponse;
import capstone_project.dtos.response.order.contract.ContractResponse;
import capstone_project.dtos.response.order.transaction.TransactionResponse;
import capstone_project.entity.auth.UserEntity;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.repository.entityServices.auth.UserEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleAssignmentEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleEntityService;
import capstone_project.service.services.order.order.JourneyHistoryService;
import capstone_project.service.services.order.seal.OrderSealService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class SimpleOrderMapper {
    private final UserEntityService userEntityService;
    private final VehicleEntityService vehicleEntityService;
    private final OrderSealService orderSealService;
    private final JourneyHistoryService journeyHistoryService;
    private final VehicleAssignmentEntityService vehicleAssignmentEntityService;

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
                    effectiveTotal = contractResponse.supportedValue();
                }
            }
        } catch (Exception ignored) {
        }
        if (effectiveTotal == null && orderResponse != null) {
            effectiveTotal = orderResponse.totalPrice();
        }

        // Convert Order with enhanced vehicle assignments
        SimpleOrderResponse simpleOrderResponse = toSimpleOrderResponseWithTripInfo(
                orderResponse,
                issueImageResponses,
                photoCompletionResponses,
                effectiveTotal
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
            BigDecimal effectiveTotal
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

        // Process order details with enhanced vehicle assignments
        List<SimpleOrderDetailResponse> simpleOrderDetails = response.orderDetails().stream()
                .map(detail -> {
                    if (detail.vehicleAssignmentId() != null) {
                        UUID vehicleAssignmentId = detail.vehicleAssignmentId().id();

                        // Get issue for this vehicle assignment
                        SimpleIssueImageResponse issue = issuesByVehicleAssignment.get(vehicleAssignmentId);

                        List<String> photoCompletions = getPhotoCompletionsFor(photoCompletionResponses, vehicleAssignmentId);

                        // Pass caches so entity lookups are memoized
                        return toSimpleOrderDetailResponseWithTripInfo(
                                detail,
                                issue,
                                photoCompletions,
                                vehicleCache,
                                userCache
                        );
                    } else {
                        return toSimpleOrderDetailResponse(detail);
                    }
                })
                .collect(Collectors.toList());

        return new SimpleOrderResponse(
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
                simpleOrderDetails
        );
    }

    private SimpleOrderDetailResponse toSimpleOrderDetailResponseWithTripInfo(
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

        SimpleVehicleAssignmentResponse vehicleAssignment = null;
        VehicleResponse vehicle = null;
        if (detail.vehicleAssignmentId() != null) {
            VehicleAssignmentEntity vaEntity = null;
            try {
                UUID vaId = detail.vehicleAssignmentId().id();
                if (vaId != null) {
                    vaEntity = vehicleAssignmentEntityService.findEntityById(vaId).orElse(null);
                }
            } catch (Exception ignored) {
            }
            SimpleDriverResponse primaryDriver = null;
            SimpleDriverResponse secondaryDriver = null;
            if (vaEntity != null) {
                try {
                    // Vehicle from assignment entity
                    if (vaEntity.getVehicleEntity() != null) {
                        var vehicleEntity = vaEntity.getVehicleEntity();
                        vehicle = new VehicleResponse(
                                vehicleEntity.getId(),
                                vehicleEntity.getManufacturer(),
                                vehicleEntity.getModel(),
                                vehicleEntity.getLicensePlateNumber(),
                                vehicleEntity.getVehicleTypeEntity() != null ? vehicleEntity.getVehicleTypeEntity().getVehicleTypeName() : null
                        );
                    } else {
                        // fallback to vehicle id in DTO -> use cached vehicle entity
                        UUID vehicleId = detail.vehicleAssignmentId().vehicleId();
                        var vehicleEntity = getVehicleFromCache(vehicleId, vehicleCache);
                        vehicle = new VehicleResponse(
                                null,
                                vehicleEntity.getManufacturer(),
                                vehicleEntity.getModel(),
                                vehicleEntity.getLicensePlateNumber(),
                                vehicleEntity.getVehicleTypeEntity() != null ? vehicleEntity.getVehicleTypeEntity().getVehicleTypeName() : null
                        );
                    }
                } catch (Exception ignored) {
                }

                try {
                    if (vaEntity.getDriver1() != null) {
                        var d = vaEntity.getDriver1();
                        primaryDriver = new SimpleDriverResponse(d.getId().toString(), d.getUser().getFullName(), d.getUser().getPhoneNumber());
                    } else {
                        UUID d1 = detail.vehicleAssignmentId().driver_id_1();
                        var d = getUserFromCache(d1, userCache);
                        if (d != null) primaryDriver = new SimpleDriverResponse(d.getId().toString(), d.getFullName(), d.getPhoneNumber());
                    }
                } catch (Exception ignored) {
                }

                try {
                    if (vaEntity.getDriver2() != null) {
                        var d = vaEntity.getDriver2();
                        secondaryDriver = new SimpleDriverResponse(d.getId().toString(), d.getUser().getFullName(), d.getUser().getPhoneNumber());
                    } else {
                        UUID d2 = detail.vehicleAssignmentId().driver_id_2();
                        var d = getUserFromCache(d2, userCache);
                        if (d != null) secondaryDriver = new SimpleDriverResponse(d.getId().toString(), d.getFullName(), d.getPhoneNumber());
                    }
                } catch (Exception ignored) {
                }
            } else {
                try {
                    UUID vehicleId = detail.vehicleAssignmentId().vehicleId();
                    var vehicleFromCache = getVehicleFromCache(vehicleId, vehicleCache);
                    vehicle = new VehicleResponse(
                            null,
                            vehicleFromCache.getManufacturer(),
                            vehicleFromCache.getModel(),
                            vehicleFromCache.getLicensePlateNumber(),
                            vehicleFromCache.getVehicleTypeEntity() != null ? vehicleFromCache.getVehicleTypeEntity().getVehicleTypeName() : null
                    );
                } catch (Exception ignored) {
                }

                try {
                    UUID d1 = detail.vehicleAssignmentId().driver_id_1();
                    var d = getUserFromCache(d1, userCache);
                    if (d != null) primaryDriver = new SimpleDriverResponse(d.getId().toString(), d.getFullName(), d.getPhoneNumber());
                } catch (Exception ignored) {
                }

                try {
                    UUID d2 = detail.vehicleAssignmentId().driver_id_2();
                    var d = getUserFromCache(d2, userCache);
                    if (d != null) secondaryDriver = new SimpleDriverResponse(d.getId().toString(), d.getFullName(), d.getPhoneNumber());
                } catch (Exception ignored) {
                }
            }

            // Retrieve journey history for the vehicle assignment (null-safe)
            List<JourneyHistoryResponse> journeyHistories = Collections.emptyList();
            try {
                UUID vehicleAssignmentId = detail.vehicleAssignmentId().id();
                List<JourneyHistoryResponse> raw = journeyHistoryService.getByVehicleAssignmentId(vehicleAssignmentId);
                if (raw != null) {
                    // Filter journey histories for customer view (only showing pickup, delivery, and intermediate points)
                    journeyHistories = filterJourneyHistoriesForCustomer(raw);
                }
            } catch (Exception e) {
                // Keep journeyHistories as empty list
            }

            // Get all order seals for this vehicle assignment (null-safe)
            List<GetOrderSealResponse> orderSeals = Collections.emptyList();
            try {
                UUID vehicleAssignmentId = detail.vehicleAssignmentId().id();
                List<GetOrderSealResponse> raw = orderSealService.getAllOrderSealsByVehicleAssignmentId(vehicleAssignmentId);
                if (raw != null) orderSeals = raw;
            } catch (Exception e) {
                // Keep orderSeals as empty list
            }

            // Ensure photoCompletions and issue lists are non-null
            List<String> safePhotoCompletions = photoCompletions != null ? photoCompletions : Collections.emptyList();
            List<SimpleIssueImageResponse> issuesList = issue != null ? List.of(issue) : Collections.emptyList();

            // Build vehicleAssignment response
            vehicleAssignment = new SimpleVehicleAssignmentResponse(
                    detail.vehicleAssignmentId().id().toString(),
                    vehicle,
                    primaryDriver,
                    secondaryDriver,
                    detail.vehicleAssignmentId().status(),
                    detail.vehicleAssignmentId().trackingCode(),
                    issuesList,
                    safePhotoCompletions,
                    orderSeals,
                    journeyHistories
            );
        }

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
                vehicleAssignment
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
                null // No vehicle assignment
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
                contract.supportedValue(),
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
     * Filter journey histories for customer view
     * Only include segments related to pickup, delivery, or intermediate points between them
     * Excludes carrier-related segments that customers don't need to see
     */
    private List<JourneyHistoryResponse> filterJourneyHistoriesForCustomer(List<JourneyHistoryResponse> rawHistories) {
        if (rawHistories == null || rawHistories.isEmpty()) {
            return Collections.emptyList();
        }

        return rawHistories.stream()
                .map(history -> {
                    // Filter journey segments to only include customer-relevant ones
                    List<JourneySegmentResponse> filteredSegments = filterJourneySegmentsForCustomer(history.journeySegments());

                    // Calculate total distance from filtered segments
                    Double totalDistance = calculateTotalDistance(filteredSegments);

                    // Create new JourneyHistoryResponse with filtered segments
                    return new JourneyHistoryResponse(
                            history.id(),
                            history.journeyName(),
                            history.journeyType(),
                            history.status(),
                            history.totalTollFee(),
                            history.totalTollCount(),
                            totalDistance,
                            history.reasonForReroute(),
                            history.vehicleAssignmentId(),
                            filteredSegments,
                            history.createdAt(),
                            history.modifiedAt()
                    );
                })
                .collect(Collectors.toList());
    }

    /**
     * Filter journey segments to only include those related to pickup, delivery, or intermediate points
     * The filtering logic uses segment names to identify relevant points for customers
     */
    private List<JourneySegmentResponse> filterJourneySegmentsForCustomer(List<JourneySegmentResponse> segments) {
        if (segments == null || segments.isEmpty()) {
            return Collections.emptyList();
        }

        return segments.stream()
                .filter(segment -> {
                    // Keep segments that contain keywords related to customer-relevant points
                    String startPointLower = segment.startPointName() != null ? segment.startPointName().toLowerCase() : "";
                    String endPointLower = segment.endPointName() != null ? segment.endPointName().toLowerCase() : "";

                    // Keywords that indicate customer-relevant points
                    boolean isPickupRelated = startPointLower.contains("pickup") || endPointLower.contains("pickup") ||
                                             startPointLower.contains("điểm đón") || endPointLower.contains("điểm đón");

                    boolean isDeliveryRelated = startPointLower.contains("delivery") || endPointLower.contains("delivery") ||
                                               startPointLower.contains("điểm giao") || endPointLower.contains("điểm giao") ||
                                               startPointLower.contains("destination") || endPointLower.contains("destination");

                    boolean isCustomerLocation = startPointLower.contains("customer") || endPointLower.contains("customer") ||
                                               startPointLower.contains("khách hàng") || endPointLower.contains("khách hàng");

                    // Exclude segments that contain carrier-related keywords
                    boolean isCarrierRelated = startPointLower.contains("carrier") || endPointLower.contains("carrier") ||
                                              startPointLower.contains("depot") || endPointLower.contains("depot") ||
                                              startPointLower.contains("garage") || endPointLower.contains("garage") ||
                                              startPointLower.contains("parking") || endPointLower.contains("parking");

                    return (isPickupRelated || isDeliveryRelated || isCustomerLocation) && !isCarrierRelated;
                })
                .collect(Collectors.toList());
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
