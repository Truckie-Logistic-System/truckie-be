package capstone_project.dtos.response.order;

import capstone_project.dtos.response.vehicle.VehicleAssignmentByJpaResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record GetOrderDetailByJpaResponse(
        String id,
        BigDecimal weight,
        BigDecimal weightBaseUnit,
        String unit,
        String description,
        String status,
        LocalDateTime startTime,
        LocalDateTime estimatedStartTime,
        LocalDateTime endTime,
        LocalDateTime estimatedEndTime,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String trackingCode,
        GetOrderSizeResponse orderSizeEntity,
        VehicleAssignmentByJpaResponse vehicleAssignmentEntity
) {
}
