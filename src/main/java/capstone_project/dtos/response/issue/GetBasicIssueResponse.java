package capstone_project.dtos.response.issue;

import capstone_project.common.enums.IssueCategoryEnum;
import capstone_project.dtos.response.order.seal.GetSealResponse;
import capstone_project.dtos.response.user.CustomerResponse;
import capstone_project.dtos.response.vehicle.VehicleAssignmentResponse;
import capstone_project.dtos.response.refund.GetRefundResponse;
import capstone_project.entity.auth.UserEntity;
import capstone_project.entity.issue.IssueTypeEntity;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record GetBasicIssueResponse (
        UUID id,
        String description,
        BigDecimal locationLatitude,
        BigDecimal locationLongitude,
        String status,
        IssueCategoryEnum issueCategory, // GENERAL, SEAL_REPLACEMENT, ACCIDENT, PENALTY, etc.
        LocalDateTime reportedAt,
        LocalDateTime resolvedAt,
        VehicleAssignmentResponse vehicleAssignmentEntity,
        UserEntity staff,
        GetIssueTypeResponse issueTypeEntity,
        
        // Seal replacement specific fields (nullable for non-seal issues)
        GetSealResponse oldSeal,
        GetSealResponse newSeal,
        String sealRemovalImage,
        String newSealAttachedImage,
        LocalDateTime newSealConfirmedAt,
        
        // Damage issue specific fields (nullable for non-damage issues)
        List<String> issueImages, // URLs of damage images
        
        // Order detail information (for damage issues)
        OrderDetailForIssueResponse orderDetail,
        
        // Sender/Customer information (for damage and order rejection issues)
        CustomerResponse sender,
        
        // ORDER_REJECTION specific fields
        LocalDateTime paymentDeadline,
        BigDecimal calculatedFee,
        BigDecimal adjustedFee,
        BigDecimal finalFee,
        List<OrderDetailForIssueResponse> affectedOrderDetails, // Multiple order details can be rejected
        GetRefundResponse returnTransaction, // The refund/transaction for return payment (deprecated)
        List<capstone_project.dtos.response.order.transaction.TransactionResponse> transactions // All payment transactions
) {
}
