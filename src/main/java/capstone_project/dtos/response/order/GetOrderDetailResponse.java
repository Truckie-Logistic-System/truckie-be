package capstone_project.dtos.response.order;

import capstone_project.dtos.response.vehicle.VehicleAssignmentResponse;
import capstone_project.entity.order.order.OrderSizeEntity;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record GetOrderDetailResponse  (
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
        String orderId,
        GetOrderSizeResponse orderSizeId,
        VehicleAssignmentResponse vehicleAssignmentId
)
{
}
