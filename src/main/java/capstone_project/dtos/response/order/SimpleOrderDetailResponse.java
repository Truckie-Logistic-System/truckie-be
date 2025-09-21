package capstone_project.dtos.response.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SimpleOrderDetailResponse(
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
    SimpleVehicleAssignmentResponse vehicleAssignment
) {}