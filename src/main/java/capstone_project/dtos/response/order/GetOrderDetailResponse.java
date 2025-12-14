package capstone_project.dtos.response.order;

import capstone_project.dtos.response.vehicle.VehicleAssignmentResponse;
import capstone_project.entity.order.order.OrderSizeEntity;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record GetOrderDetailResponse  (
        BigDecimal weight,
        BigDecimal weightBaseUnit,
        String unit,
        String description,
        String status,
        LocalDateTime estimatedStartTime,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String trackingCode,
        String orderId,
        GetOrderSizeResponse orderSizeId,
        UUID vehicleAssignmentId,  // Changed from full object to ID reference
        BigDecimal declaredValue   // Giá trị khai báo của kiện hàng
)
{
}
