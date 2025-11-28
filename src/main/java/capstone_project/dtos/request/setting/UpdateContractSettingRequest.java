package capstone_project.dtos.request.setting;

import java.math.BigDecimal;

public record UpdateContractSettingRequest(

        BigDecimal depositPercent,
        Integer expiredDepositDate,
        BigDecimal insuranceRateNormal,
        BigDecimal insuranceRateFragile,
        BigDecimal vatRate
) {
}
