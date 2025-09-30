package capstone_project.dtos.response.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record GetOrderForDriverResponse(
        String id,
        BigDecimal totalPrice,
        String notes,
        Integer totalQuantity,
        String orderCode,
        String receiverName,
        String receiverPhone,
        String receiverIdentity,
        String packageDescription,
        LocalDateTime createdAt,
        String status
) {
}
