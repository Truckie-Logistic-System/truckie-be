package capstone_project.dtos.request.order;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateContractForCusRequest(
        String contractName,

        LocalDateTime effectiveDate,

        LocalDateTime expirationDate,

        BigDecimal adjustedValue,

        String description,

        String attachFileUrl,

        @NotBlank(message = "Order ID must not be blank")
        String orderId
) {
}
