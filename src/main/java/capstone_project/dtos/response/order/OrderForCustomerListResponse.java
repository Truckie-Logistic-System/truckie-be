package capstone_project.dtos.response.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrderForCustomerListResponse(
        UUID id,
        String orderCode,
        BigDecimal totalPrice,
        Integer totalQuantity,
        String status,
        String notes,
        String packageDescription,
        String receiverName,
        String receiverPhone,
        String pickupAddress,
        String deliveryAddress,
        LocalDateTime createdAt
) {}
