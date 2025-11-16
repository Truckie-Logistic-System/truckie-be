package capstone_project.dtos.response.issue;

import capstone_project.common.enums.IssueCategoryEnum;
import capstone_project.dtos.response.order.transaction.TransactionResponse;
import capstone_project.dtos.response.order.seal.GetSealResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record SimpleIssueResponse(
    String id,
    String description,
    BigDecimal locationLatitude,
    BigDecimal locationLongitude,
    String status,
    String vehicleAssignmentId,
    SimpleStaffResponse staff,
    String issueTypeName,
    String issueTypeDescription, // Description from IssueType
    LocalDateTime reportedAt, // When the issue was reported
    IssueCategoryEnum issueCategory, // GENERAL, ORDER_REJECTION, DAMAGE, etc.
    
    // Issue images
    List<String> issueImages,
    
    // SEAL_REPLACEMENT specific fields
    GetSealResponse oldSeal,
    GetSealResponse newSeal,
    String sealRemovalImage,
    String newSealAttachedImage,
    LocalDateTime newSealConfirmedAt,
    
    // ORDER_REJECTION specific fields
    LocalDateTime paymentDeadline,
    BigDecimal calculatedFee,
    BigDecimal adjustedFee,
    BigDecimal finalFee,
    List<OrderDetailForIssueResponse> affectedOrderDetails,
    capstone_project.dtos.response.refund.GetRefundResponse refund, // Refund record
    TransactionResponse transaction, // Payment transaction (deprecated - use transactions list)
    List<TransactionResponse> transactions // All payment transactions (PENDING, FAILED, PAID)
) {
    // Constructor for backward compatibility (non-ORDER_REJECTION issues)
    public SimpleIssueResponse(
        String id,
        String description,
        BigDecimal locationLatitude,
        BigDecimal locationLongitude,
        String status,
        String vehicleAssignmentId,
        SimpleStaffResponse staff,
        String issueTypeName
    ) {
        this(id, description, locationLatitude, locationLongitude, status, 
             vehicleAssignmentId, staff, issueTypeName,
             null, // issueTypeDescription
             null, // reportedAt
             null, // issueCategory
             null, // issueImages
             null, null, null, null, null, // seal fields
             null, null, null, null, null, null, null, null); // ORDER_REJECTION fields (added transactions list)
    }
}