package capstone_project.dtos.response.order;

import capstone_project.dtos.response.user.AddressResponse;
import capstone_project.dtos.response.user.CustomerResponse;
import capstone_project.entity.order.order.OrderDetailEntity;
import capstone_project.entity.user.address.AddressEntity;
import capstone_project.entity.user.customer.CustomerEntity;

import java.math.BigDecimal;
import java.util.List;

public record GetOrderResponse(
    String id,
    BigDecimal totalPrice,
    String notes,
    Integer totalQuantity,
    String orderCode,
    String receiverName,
    String receiverPhone,
    String packageDescription,
    AddressResponse deliveryAddress,
    AddressResponse pickupAddress,
    CustomerResponse sender,
    List<GetOrderDetailResponse> orderDetails
) {
}
