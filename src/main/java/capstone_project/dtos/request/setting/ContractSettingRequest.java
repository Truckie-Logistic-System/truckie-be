package capstone_project.dtos.request.setting;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ContractSettingRequest(

        @NotNull(message = "Deposit percent must not be null")
        @Min(value = 0L, message = "Deposit percent must be positive")
        BigDecimal depositPercent,

        @NotNull(message = "Deposit deadline hours must not be null")
        @Min(value = 1L, message = "Deposit deadline hours must be at least 1")
        Integer depositDeadlineHours,

        @NotNull(message = "Signing deadline hours must not be null")
        @Min(value = 1L, message = "Signing deadline hours must be at least 1")
        Integer signingDeadlineHours,

        @NotNull(message = "Full payment days before pickup must not be null")
        @Min(value = 0L, message = "Full payment days before pickup must be non-negative")
        Integer fullPaymentDaysBeforePickup,

        @NotNull(message = "Insurance rate for normal goods must not be null")
        @Min(value = 0L, message = "Insurance rate must be positive")
        BigDecimal insuranceRateNormal,

        @NotNull(message = "Insurance rate for fragile goods must not be null")
        @Min(value = 0L, message = "Insurance rate must be positive")
        BigDecimal insuranceRateFragile,

        BigDecimal vatRate
) {
}
