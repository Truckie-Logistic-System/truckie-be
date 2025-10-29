package capstone_project.dtos.response.order;

import capstone_project.dtos.response.user.GetAddressForOrderResponse;
import capstone_project.dtos.response.user.GetCustomerForOrderResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record GetOrderByJpaResponse(
        String id,
        String notes,
        Integer totalQuantity,
        String orderCode,
        String receiverName,
        String receiverPhone,
        String receiverIdentity,
        String packageDescription,
        LocalDateTime createdAt,
        String status,
        GetAddressForOrderResponse deliveryAddress,
        GetAddressForOrderResponse pickupAddress,
        GetCustomerForOrderResponse sender,
        CategoryResponse category,
        List<GetOrderDetailByJpaResponse> orderDetailEntities
) {
}
