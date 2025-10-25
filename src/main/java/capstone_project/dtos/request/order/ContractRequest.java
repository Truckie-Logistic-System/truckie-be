package capstone_project.dtos.request.order;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ContractRequest(
        @NotBlank(message = "Contract name must not be blank")
        String contractName,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime effectiveDate,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime expirationDate,

        @Min(value = 0L, message = "The num of adjusted value must be positive")
        BigDecimal adjustedValue,

//        @NotNull(message = "Total value must not be blank")
//        @DecimalMin(value = "0.0", inclusive = true, message = "Total value must be >= 0")
//        BigDecimal totalValue,

        String description,

        String attachFileUrl,

        @NotBlank(message = "Order ID must not be blank")
        String orderId

//        @NotBlank(message = "Staff ID must not be blank")
//        String staffId
) {
}
