package capstone_project.dtos.request.setting;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ContractSettingRequest(

        @NotNull(message = "Deposit percent must not be null")
        @Min(value = 0L, message = "Deposit percent must be positive")
        BigDecimal depositPercent,

        @NotNull(message = "Expired deposit date must not be null")
        @Min(value = 0L, message = "Expired deposit date must be positive")
        Integer expiredDepositDate,

        @NotNull(message = "Insurance rate must not be null")
        @Min(value = 0L, message = "Insurance rate must be positive")
        BigDecimal insuranceRate
) {
}
