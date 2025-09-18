package capstone_project.dtos.response.setting;

import java.math.BigDecimal;

public record ContractSettingResponse(

        String id,
        BigDecimal depositPercent,
        Integer expiredDepositDate,
        BigDecimal insuranceRate
) {
}
