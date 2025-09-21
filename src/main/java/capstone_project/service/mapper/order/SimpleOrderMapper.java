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
import capstone_project.entity.order.order.OrderDetailEntity;
import capstone_project.entity.order.order.OrderEntity;
import capstone_project.entity.order.order.OrderSizeEntity;
import capstone_project.repository.entityServices.auth.UserEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleEntityService;
import capstone_project.service.services.order.seal.OrderSealService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SimpleOrderMapper {
    private final UserEntityService userEntityService;
    private final VehicleEntityService vehicleEntityService;
    private final OrderSealService orderSealService;

    public SimpleOrderForCustomerResponse toSimpleOrderForCustomerResponse(
            GetOrderResponse orderResponse,
            List<GetIssueImageResponse> issueImageResponses,
            Map<UUID, List<PhotoCompletionResponse>> photoCompletionResponses,
            ContractResponse contractResponse,
            List<TransactionResponse> transactionResponses
    ) {
        // Convert Order with enhanced vehicle assignments
        SimpleOrderResponse simpleOrderResponse = toSimpleOrderResponseWithTripInfo(
                orderResponse,
                issueImageResponses,
                photoCompletionResponses
        );

        // Convert Contract
        SimpleContractResponse simpleContractResponse = contractResponse != null ? 
                toSimpleContractResponse(contractResponse) : null;
        
        // Convert Transactions
        List<SimpleTransactionResponse> simpleTransactionResponses = transactionResponses.stream()
                .map(this::toSimpleTransactionResponse)
                .collect(Collectors.toList());
        
        return new SimpleOrderForCustomerResponse(
                simpleOrderResponse,
                simpleContractResponse,
                simpleTransactionResponses
        );
    }

    private SimpleOrderResponse toSimpleOrderResponseWithTripInfo(
            GetOrderResponse response,
            List<GetIssueImageResponse> issueImageResponses,
            Map<UUID, List<PhotoCompletionResponse>> photoCompletionResponses
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
        
        // Create a map of issues by vehicle assignment ID for easy lookup
        Map<UUID, SimpleIssueImageResponse> issuesByVehicleAssignment = new HashMap<>();
        for (GetIssueImageResponse issueImageResponse : issueImageResponses) {
            if (issueImageResponse != null && issueImageResponse.issue() != null) {
                try {
                    UUID vehicleAssignmentId = UUID.fromString(issueImageResponse.issue().vehicleAssignmentEntity().id().toString());
                    issuesByVehicleAssignment.put(vehicleAssignmentId, toSimpleIssueImageResponse(issueImageResponse));
                } catch (Exception e) {
                    // Handle invalid UUID format gracefully
                }
            }
        }

        // Process order details with enhanced vehicle assignments
        List<SimpleOrderDetailResponse> simpleOrderDetails = response.orderDetails().stream()
                .map(detail -> {
                    // Create an enhanced version of SimpleOrderDetailResponse with trip info integrated into vehicle assignment
                    if (detail.vehicleAssignmentId() != null) {
                        UUID vehicleAssignmentId = detail.vehicleAssignmentId().id();

                        // Get issue for this vehicle assignment
                        SimpleIssueImageResponse issue = issuesByVehicleAssignment.get(vehicleAssignmentId);

                        // Get photo completions for this vehicle assignment
                        List<String> photoCompletions = null;
                        if (photoCompletionResponses.containsKey(vehicleAssignmentId)) {
                            photoCompletions = photoCompletionResponses.get(vehicleAssignmentId).stream()
                                    .map(PhotoCompletionResponse::imageUrl)
                                    .collect(Collectors.toList());
                        }

                        // Create enhanced vehicle assignment with trip information
                        return toSimpleOrderDetailResponseWithTripInfo(
                                detail,
                                issue,
                                photoCompletions
                        );
                    } else {
                        return toSimpleOrderDetailResponse(detail);
                    }
                })
                .collect(Collectors.toList());
        
        return new SimpleOrderResponse(
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
                simpleOrderDetails
        );
    }
    
    private SimpleOrderDetailResponse toSimpleOrderDetailResponseWithTripInfo(
            GetOrderDetailResponse detail,
            SimpleIssueImageResponse issue,
            List<String> photoCompletions
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
        if (detail.vehicleAssignmentId() != null) {
            // Get vehicle information
            String vehicleName = "";
            String licensePlate = "";
            try {
                var vehicle = vehicleEntityService.findEntityById(detail.vehicleAssignmentId().vehicleId())
                        .orElse(null);
                if (vehicle != null) {
                    vehicleName = vehicle.getModel() + " " + vehicle.getManufacturer();
                    licensePlate = vehicle.getLicensePlateNumber();
                }
            } catch (Exception e) {
                // Handle exception gracefully
            }

            // Get primary driver information
            SimpleDriverResponse primaryDriver = null;
            if (detail.vehicleAssignmentId().driver_id_1() != null) {
                try {
                    var driver = userEntityService.findEntityById(detail.vehicleAssignmentId().driver_id_1())
                            .orElse(null);
                    if (driver != null) {
                        primaryDriver = new SimpleDriverResponse(
                                driver.getId().toString(),
                                driver.getFullName(),
                                driver.getPhoneNumber()
                        );
                    }
                } catch (Exception e) {
                    // Handle exception gracefully
                }
            }
            
            // Get secondary driver information
            SimpleDriverResponse secondaryDriver = null;
            if (detail.vehicleAssignmentId().driver_id_2() != null) {
                try {
                    var driver = userEntityService.findEntityById(detail.vehicleAssignmentId().driver_id_2())
                            .orElse(null);
                    if (driver != null) {
                        secondaryDriver = new SimpleDriverResponse(
                                driver.getId().toString(),
                                driver.getFullName(),
                                driver.getPhoneNumber()
                        );
                    }
                } catch (Exception e) {
                    // Handle exception gracefully
                }
            }

            // Create journey history (placeholder - implement actual journey history retrieval)
            List<JourneyHistoryResponse> journeyHistory = new ArrayList<>();

            // Get all order seals for this vehicle assignment
            List<GetOrderSealResponse> orderSeals = null;
            try {
                UUID vehicleAssignmentId = detail.vehicleAssignmentId().id();
                orderSeals = orderSealService.getAllOrderSealsByVehicleAssignmentId(vehicleAssignmentId);
            } catch (Exception e) {
                // Handle exception gracefully
                orderSeals = new ArrayList<>();
            }

            // Create enhanced vehicle assignment with trip information
            vehicleAssignment = new SimpleVehicleAssignmentResponse(
                    detail.vehicleAssignmentId().id().toString(),
                    vehicleName,
                    licensePlate,
                    primaryDriver,
                    secondaryDriver,
                    detail.vehicleAssignmentId().status(),
                    issue != null ? List.of(issue) : null,
                    photoCompletions,
                    orderSeals,
                    journeyHistory
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
}
