package capstone_project.dtos.response.order;

import capstone_project.dtos.response.vehicle.VehicleAssignmentResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record GetOrderDetailsResponseForList(
        BigDecimal weight,
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
        String orderSizeId,
        String vehicleAssignmentId
) {
}
