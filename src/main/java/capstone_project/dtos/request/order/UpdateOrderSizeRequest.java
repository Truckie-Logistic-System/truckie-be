package capstone_project.dtos.request.order;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.UUID;


import java.math.BigDecimal;


public record UpdateOrderSizeRequest (
        @UUID
        @NotNull
        String id,

        @NotNull(message = "Min weight is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Min weight must be greater than 0")
        BigDecimal minWeight,

        @NotNull(message = "Max weight is required")
        BigDecimal maxWeight,

        @NotNull(message = "Min length is required")
        BigDecimal minLength,

        @NotNull(message = "Max length is required")
        BigDecimal maxLength,

        @NotNull(message = "Min height is required")
        BigDecimal minHeight,

        @NotNull(message = "Max height is required")
        BigDecimal maxHeight,

        @NotNull(message = "Min width is required")
        BigDecimal minWidth,

        @NotNull(message = "Max width is required")
        BigDecimal maxWidth,

        @Size(max = 200, message = "Description must not exceed 200 characters")
        String description
){
}
