package capstone_project.dtos.response.issue;

import capstone_project.dtos.response.order.JourneyHistoryResponse;
import capstone_project.dtos.response.order.transaction.TransactionResponse;
import capstone_project.dtos.response.user.CustomerInfoResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for ORDER_REJECTION issue detail
 * Contains full information for staff and customer to view and process
 */
public record OrderRejectionDetailResponse(
        UUID issueId,
        
        String issueCode,
        
        String description,
        
        String status, // OPEN, IN_PROGRESS, RESOLVED
        
        LocalDateTime reportedAt,
        
        LocalDateTime resolvedAt,
        
        // Customer information (sender/owner of the order)
        CustomerInfoResponse customerInfo,
        
        // Contract ID for creating payment link
        UUID contractId,
        
        // Return shipping fee
        BigDecimal calculatedFee,
        
        BigDecimal adjustedFee,
        
        BigDecimal finalFee,
        
        // Transaction for return shipping payment
        TransactionResponse returnTransaction,
        
        // Payment deadline
        LocalDateTime paymentDeadline,
        
        // New journey for returning goods
        JourneyHistoryResponse returnJourney,
        
        // Order details being returned
        List<OrderDetailForIssueResponse> affectedOrderDetails,
        
        // Return delivery confirmation images (multiple photos allowed)
        List<String> returnDeliveryImages
) {
}
