package capstone_project.dtos.response.order;

import capstone_project.dtos.response.order.contract.SimpleContractResponse;
import capstone_project.dtos.response.order.transaction.SimpleTransactionResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Enhanced order response with full information for staff
 */
public record StaffOrderResponse(
    String id,
    BigDecimal totalPrice,
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
    String senderRepresentativeName,
    String senderRepresentativePhone,
    String senderCompanyName,
    String categoryName,
    List<StaffOrderDetailResponse> orderDetails,
    List<StaffVehicleAssignmentResponse> vehicleAssignments  // Moved from orderDetail level
) {}
