package capstone_project.dtos.response.setting;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ContractSettingResponse(

        String id,
        BigDecimal depositPercent,
        LocalDateTime expiredDepositDate,
        BigDecimal insuranceRate
) {
}
