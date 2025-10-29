package capstone_project.dtos.response.order;

import capstone_project.dtos.response.user.AddressResponse;
import capstone_project.dtos.response.user.CustomerResponse;
import capstone_project.dtos.response.vehicle.VehicleAssignmentResponse;
import capstone_project.entity.order.order.OrderDetailEntity;
import capstone_project.entity.user.address.AddressEntity;
import capstone_project.entity.user.customer.CustomerEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record GetOrderResponse(
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
    AddressResponse deliveryAddress,
    AddressResponse pickupAddress,
    CustomerResponse sender,
    CategoryResponse category,
    List<GetOrderDetailResponse> orderDetails,
    List<VehicleAssignmentResponse> vehicleAssignments  // Moved from orderDetail level
) {
}
