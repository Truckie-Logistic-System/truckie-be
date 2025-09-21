package capstone_project.dtos.response.order;

import capstone_project.dtos.response.issue.SimpleIssueImageResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Enhanced order detail response with full information for staff
 */
public record StaffOrderDetailResponse(
    String id,
    BigDecimal weightBaseUnit,
    String unit,
    String description,
    String status,
    LocalDateTime startTime,
    LocalDateTime estimatedStartTime,
    LocalDateTime endTime,
    LocalDateTime estimatedEndTime,
    LocalDateTime createdAt,
    String trackingCode,
    SimpleOrderSizeResponse orderSize,
    StaffVehicleAssignmentResponse vehicleAssignment
) {}
