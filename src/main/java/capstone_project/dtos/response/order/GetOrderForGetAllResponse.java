package capstone_project.dtos.response.order;

import capstone_project.dtos.response.user.AddressResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record GetOrderForGetAllResponse (
        UUID id,
        BigDecimal totalPrice,
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
        AddressResponse deliveryAddress,
        AddressResponse pickupAddress,
        String pickupAddressId,
        String categoryId
){
}
