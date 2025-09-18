package capstone_project.dtos.request.order;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateOrderSizeRequest (
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
