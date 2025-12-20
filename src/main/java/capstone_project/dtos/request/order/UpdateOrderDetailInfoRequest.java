package capstone_project.dtos.request.order;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.UUID;

import java.math.BigDecimal;

public record UpdateOrderDetailInfoRequest(
        // ID của OrderDetail cần update (null nếu là tạo mới)
        String orderDetailId,

        // Số lượng kiện hàng (mặc định là 1)
        @NotNull(message = "Quantity cannot be null")
        @Min(value = 1L, message = "Quantity must be at least 1")
        Integer quantity,

        @NotNull(message = "Weight cannot be null")
        @Min(value = 0L, message = "Weight must be positive")
        BigDecimal weight,

        @NotBlank(message = "Unit cannot be blank")
        String unit,

        @NotBlank(message = "Description cannot be blank")
        String description,

        @NotBlank(message = "Order size ID cannot be blank")
        @UUID(message = "Order size ID must be a valid UUID")
        String orderSizeId,

        // Giá trị khai báo của kiện hàng (VNĐ) - dùng để tính phí bảo hiểm
        @NotNull(message = "Declared value cannot be null")
        @Min(value = 0L, message = "Declared value must be positive or zero")
        BigDecimal declaredValue,

        // Flag để đánh dấu OrderDetail cần xóa
        Boolean toDelete
) {}
