package capstone_project.dtos.response.pricing;

import java.math.BigDecimal;

public record BasingPriceResponse(

        String id,
        BigDecimal basePrice,

        String sizeRuleId,
        String distanceRuleId

) {
}
