package capstone_project.dtos.request.setting;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ContractSettingRequest(

        @NotNull(message = "Deposit percent must not be null")
        @Min(value = 0L, message = "Deposit percent must be positive")
        BigDecimal depositPercent,

        @NotNull(message = "Expired deposit date must not be null")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime expiredDepositDate,

        @NotNull(message = "Insurance rate must not be null")
        @Min(value = 0L, message = "Insurance rate must be positive")
        BigDecimal insuranceRate
) {
}
