package capstone_project.dtos.response.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CreateOrderResponse(
        UUID id,
        String notes,
        int totalQuantity,
        BigDecimal totalWeight,
        String orderCode,
        String receiverName,
        String receiverPhone,
        String status,
        String packageDescription,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String senderId,
        String deliveryId,
        String pickupAddressId,
        String categoryId
) {
}
