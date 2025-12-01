package capstone_project.dtos.response.order;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Simple order info for vehicle assignment detail page
 */
public record SimpleOrderInfo(
    UUID id,
    String orderCode,
    String status,
    LocalDateTime createdAt,
    String receiverName,
    String receiverPhone,
    String deliveryAddress,
    String pickupAddress,
    String senderName,
    String companyName
) {}
