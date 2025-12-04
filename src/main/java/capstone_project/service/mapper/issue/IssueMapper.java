package capstone_project.service.mapper.issue;

import capstone_project.dtos.response.issue.GetBasicIssueResponse;
import capstone_project.dtos.response.issue.GetIssueTypeResponse;
import capstone_project.dtos.response.issue.OrderDetailForIssueResponse;
import capstone_project.dtos.response.user.CustomerResponse;
import capstone_project.entity.issue.IssueEntity;
import capstone_project.entity.issue.IssueTypeEntity;
import capstone_project.entity.order.order.OrderDetailEntity;
import capstone_project.entity.user.customer.CustomerEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = {
    capstone_project.service.mapper.vehicle.VehicleAssignmentMapper.class,
    capstone_project.service.mapper.order.JourneyHistoryMapper.class
})
public interface IssueMapper {
    GetIssueTypeResponse toIssueTypeResponse(IssueTypeEntity issueType);

    List<GetIssueTypeResponse> toIssueTypeResponses(List<IssueTypeEntity> issueTypes);

    @Mapping(source = "issueTypeEntity.issueCategory", target = "issueCategory")
    @Mapping(source = "issueTypeEntity", target = "issueTypeEntity")
    @Mapping(source = "vehicleAssignmentEntity", target = "vehicleAssignmentEntity")
    @Mapping(target = "orderDetail", expression = "java(mapOrderDetail(issue))")
    @Mapping(target = "issueImages", expression = "java(mapIssueImages(issue))")
    @Mapping(target = "sender", expression = "java(mapSender(issue))")
    @Mapping(target = "affectedOrderDetails", expression = "java(mapAffectedOrderDetails(issue))")
    @Mapping(target = "returnTransaction", expression = "java(mapReturnTransaction(issue))")
    @Mapping(source = "returnShippingFee", target = "calculatedFee")
    @Mapping(source = "adjustedReturnFee", target = "adjustedFee")
    @Mapping(target = "finalFee", expression = "java(calculateFinalFee(issue))")
    @Mapping(target = "affectedSegment", expression = "java(mapAffectedSegment(issue))")
    @Mapping(source = "reroutedJourney", target = "reroutedJourney")
    @Mapping(target = "damageCompensation", expression = "java(mapDamageCompensation(issue))")
    GetBasicIssueResponse toIssueBasicResponse(IssueEntity issue);

    List<GetBasicIssueResponse> toIssueBasicResponses(List<IssueEntity> issues);
    
    // Map order detail for issue response
    default OrderDetailForIssueResponse mapOrderDetail(IssueEntity issue) {
        if (issue.getOrderDetails() == null || issue.getOrderDetails().isEmpty()) {
            return null;
        }
        // Return the first order detail affected by this issue
        OrderDetailEntity orderDetail = issue.getOrderDetails().get(0);
        String orderId = orderDetail.getOrderEntity() != null ? 
                        orderDetail.getOrderEntity().getId().toString() : null;
        return new OrderDetailForIssueResponse(
            orderDetail.getTrackingCode(),
            orderDetail.getDescription(),
            orderDetail.getWeightBaseUnit(),
            orderDetail.getUnit(),
            orderId
        );
    }
    
    // Map issue images to list of URLs
    default List<String> mapIssueImages(IssueEntity issue) {
        if (issue.getIssueImages() == null || issue.getIssueImages().isEmpty()) {
            return List.of();
        }
        return issue.getIssueImages().stream()
            .map(img -> img.getImageUrl())
            .toList();
    }
    
    // Map sender (customer) for issue response
    // Provides contact information for staff to communicate with customer about damage/order rejection
    default CustomerResponse mapSender(IssueEntity issue) {
        if (issue.getOrderDetails() == null || issue.getOrderDetails().isEmpty()) {
            return null;
        }
        
        // Get the first order detail
        OrderDetailEntity orderDetail = issue.getOrderDetails().get(0);
        if (orderDetail.getOrderEntity() == null || orderDetail.getOrderEntity().getSender() == null) {
            return null;
        }
        
        CustomerEntity sender = orderDetail.getOrderEntity().getSender();
        
        // Map UserResponse if user exists
        capstone_project.dtos.response.auth.UserResponse userResponse = null;
        if (sender.getUser() != null) {
            var user = sender.getUser();
            userResponse = capstone_project.dtos.response.auth.UserResponse.builder()
                .id(user.getId().toString())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .build();
        }
        
        // Map to CustomerResponse with essential contact information
        return CustomerResponse.builder()
            .id(sender.getId().toString())
            .companyName(sender.getCompanyName())
            .representativeName(sender.getRepresentativeName())
            .representativePhone(sender.getRepresentativePhone())
            .businessLicenseNumber(sender.getBusinessLicenseNumber())
            .businessAddress(sender.getBusinessAddress())
            .status(sender.getStatus())
            .userResponse(userResponse)
            .build();
    }
    
    // Map all affected order details for ORDER_REJECTION
    default List<OrderDetailForIssueResponse> mapAffectedOrderDetails(IssueEntity issue) {
        if (issue.getOrderDetails() == null || issue.getOrderDetails().isEmpty()) {
            return null;
        }
        // Return all order details affected by this issue
        return issue.getOrderDetails().stream()
            .map(orderDetail -> {
                String orderId = orderDetail.getOrderEntity() != null ? 
                                orderDetail.getOrderEntity().getId().toString() : null;
                return new OrderDetailForIssueResponse(
                    orderDetail.getTrackingCode(),
                    orderDetail.getDescription(),
                    orderDetail.getWeightBaseUnit(),
                    orderDetail.getUnit(),
                    orderId
                );
            })
            .toList();
    }
    
    // Calculate final fee for ORDER_REJECTION
    default java.math.BigDecimal calculateFinalFee(IssueEntity issue) {
        // Final fee = adjusted fee if present, otherwise calculated fee
        if (issue.getAdjustedReturnFee() != null) {
            return issue.getAdjustedReturnFee();
        }
        return issue.getReturnShippingFee();
    }
    
    // Map affected segment for REROUTE
    default capstone_project.dtos.response.order.JourneySegmentResponse mapAffectedSegment(IssueEntity issue) {
        if (issue.getAffectedSegment() == null) {
            return null;
        }
        
        var segment = issue.getAffectedSegment();
        return new capstone_project.dtos.response.order.JourneySegmentResponse(
            segment.getId(),
            segment.getSegmentOrder(),
            segment.getStartPointName(),
            segment.getEndPointName(),
            segment.getStartLatitude(),
            segment.getStartLongitude(),
            segment.getEndLatitude(),
            segment.getEndLongitude(),
            segment.getDistanceKilometers(),
            segment.getPathCoordinatesJson(),
            segment.getTollDetailsJson(),
            segment.getStatus(),
            segment.getCreatedAt(),
            segment.getModifiedAt()
        );
    }
    
    // Map return transaction/refund for ORDER_REJECTION
    default capstone_project.dtos.response.refund.GetRefundResponse mapReturnTransaction(IssueEntity issue) {
        if (issue.getRefund() == null) {
            return null;
        }
        
        var refund = issue.getRefund();
        
        // Map processedBy staff
        capstone_project.dtos.response.refund.GetRefundResponse.StaffInfo staffInfo = null;
        if (refund.getProcessedByStaff() != null) {
            staffInfo = new capstone_project.dtos.response.refund.GetRefundResponse.StaffInfo(
                refund.getProcessedByStaff().getId(),
                refund.getProcessedByStaff().getFullName(),
                refund.getProcessedByStaff().getEmail()
            );
        }
        
        return new capstone_project.dtos.response.refund.GetRefundResponse(
            refund.getId(),
            refund.getRefundAmount(),
            refund.getBankTransferImage(),
            refund.getBankName(),
            refund.getAccountNumber(),
            refund.getAccountHolderName(),
            refund.getTransactionCode(),
            refund.getRefundDate(),
            refund.getNotes(),
            issue.getId(), // issueId
            staffInfo,
            refund.getCreatedAt()
        );
    }
    
    // Map DAMAGE compensation details
    default capstone_project.dtos.response.issue.DamageCompensationResponse mapDamageCompensation(IssueEntity issue) {
        // Only map for DAMAGE issues that have compensation data
        if (issue.getIssueTypeEntity() == null || 
            !capstone_project.common.enums.IssueCategoryEnum.DAMAGE.name().equals(issue.getIssueTypeEntity().getIssueCategory())) {
            return null;
        }
        
        // Get hasInsurance from Order
        Boolean hasInsurance = false;
        if (issue.getOrderDetails() != null && !issue.getOrderDetails().isEmpty()) {
            var orderDetail = issue.getOrderDetails().get(0);
            if (orderDetail.getOrderEntity() != null) {
                hasInsurance = orderDetail.getOrderEntity().getHasInsurance();
            }
        }
        
        // Determine case and labels
        String caseLabel = null;
        String caseDescription = null;
        Boolean appliesLegalLimit = null;
        
        if (issue.getDamageCompensationCase() != null) {
            try {
                var compensationCase = capstone_project.common.enums.DamageCompensationCaseEnum.valueOf(issue.getDamageCompensationCase());
                caseLabel = compensationCase.getLabel();
                caseDescription = compensationCase.getDescription();
                appliesLegalLimit = compensationCase.appliesLegalLimit();
            } catch (IllegalArgumentException e) {
                // Invalid case enum, ignore
            }
        }
        
        // Get status label
        String statusLabel = null;
        if (issue.getDamageCompensationStatus() != null) {
            try {
                var status = capstone_project.common.enums.DamageCompensationStatusEnum.valueOf(issue.getDamageCompensationStatus());
                statusLabel = status.getLabel();
            } catch (IllegalArgumentException e) {
                // Invalid status enum, ignore
            }
        }
        
        return new capstone_project.dtos.response.issue.DamageCompensationResponse(
            issue.getDamageAssessmentPercent(),
            hasInsurance,
            issue.getDamageHasDocuments(),
            issue.getDamageDeclaredValue(),
            issue.getDamageEstimatedMarketValue(),
            issue.getDamageFreightFee(),
            issue.getDamageLegalLimit(),
            issue.getDamageEstimatedLoss(),
            issue.getDamagePolicyCompensation(),
            issue.getDamageFinalCompensation(),
            issue.getDamageCompensationCase(),
            caseLabel,
            caseDescription,
            appliesLegalLimit,
            issue.getDamageAdjustReason(),
            issue.getDamageHandlerNote(),
            issue.getDamageCompensationStatus(),
            statusLabel
        );
    }

}
