package capstone_project.dtos.response.order;

/**
 * Response DTO for recipient order tracking
 * Contains order information without sensitive contract/transaction data
 * Used for public tracking by recipients (no authentication required)
 */
public record RecipientOrderTrackingResponse(
    SimpleOrderResponse order
) {}
