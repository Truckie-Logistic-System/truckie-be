package capstone_project.dtos.response.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record SimpleOrderResponse(
    String id,
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
    String deliveryAddressId,    // ID for form prefill
    String pickupAddress,
    String pickupAddressId,      // ID for form prefill
    String senderName,
    String senderPhone,
    String senderCompanyName,
    String categoryName,
    String categoryId,           // ID for form prefill
    String categoryDescription, // Category description from backend
    Boolean hasInsurance,           // Khách hàng có mua bảo hiểm hay không
    BigDecimal totalInsuranceFee,   // Tổng phí bảo hiểm (đã bao gồm VAT)
    BigDecimal totalDeclaredValue,  // Tổng giá trị khai báo của tất cả kiện hàng
    List<SimpleOrderDetailResponse> orderDetails,
    List<SimpleVehicleAssignmentResponse> vehicleAssignments
) {}