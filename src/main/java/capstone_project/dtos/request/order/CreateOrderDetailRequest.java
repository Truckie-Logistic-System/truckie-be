package capstone_project.dtos.request.order;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.UUID;

import java.math.BigDecimal;

public record CreateOrderDetailRequest(
        @NotNull(message = "Weight cannot be null")
        @Min(value = 0L, message = "Weight must be positive")
        BigDecimal weight,

        @NotBlank(message = "Description cannot be blank")
        String description,

        @NotBlank(message = "Order size ID cannot be blank")
        @UUID(message = "Order size ID must be a valid UUID")
        String orderSizeId
) {}
