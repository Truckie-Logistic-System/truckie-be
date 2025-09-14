package capstone_project.dtos.request.setting;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UpdateContractSettingRequest(

        BigDecimal depositPercent,
        LocalDateTime expiredDepositDate,
        BigDecimal insuranceRate
) {
}
