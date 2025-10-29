package capstone_project.dtos.response.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

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
    UUID vehicleAssignmentId
) {}