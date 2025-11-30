package capstone_project.dtos.response.issue;

import java.math.BigDecimal;

// Simple DTO for order detail in issue response
public record OrderDetailForIssueResponse(
        String trackingCode,
        String description,
        BigDecimal weightBaseUnit,
        String unit,
        String orderId // Added for frontend grouping logic
) {
}