package capstone_project.dtos.response.order;

import capstone_project.dtos.response.issue.SimpleIssueImageResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Enhanced order detail response with full information for staff
 */
public record StaffOrderDetailResponse(
    String id,
    BigDecimal weightBaseUnit,
    String unit,
    String description,
    String status,
    LocalDateTime estimatedStartTime,
    LocalDateTime createdAt,
    String trackingCode,
    SimpleOrderSizeResponse orderSize,
    UUID vehicleAssignmentId  // Changed from full object to ID reference
) {}
