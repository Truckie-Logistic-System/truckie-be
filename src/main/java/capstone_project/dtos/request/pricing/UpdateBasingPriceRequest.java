package capstone_project.dtos.request.pricing;

import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

public record UpdateBasingPriceRequest(

        @DecimalMin(value = "0.0", inclusive = true, message = "Basing price must be >= 0")
        BigDecimal basePrice,

        String sizeRuleId,

        String distanceRuleId
) {
}
