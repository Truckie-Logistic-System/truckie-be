package capstone_project.dtos.response.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record SimpleOrderResponse(
    String id,
    BigDecimal totalPrice,
    BigDecimal depositAmount,
    String notes,
    int totalQuantity,
    String orderCode,
    String receiverName,
    String receiverPhone,
    String receiverIdentity,
    String packageDescription,
    LocalDateTime createdAt,
    String status,
    String deliveryAddress,
    String pickupAddress,
    String senderName,
    String senderPhone,
    String senderCompanyName,
    String categoryName,
    List<SimpleOrderDetailResponse> orderDetails,
    List<SimpleVehicleAssignmentResponse> vehicleAssignments
) {}