package capstone_project.dtos.request.pricing;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record BasingPriceRequest(

        @NotNull(message = "Basing price cannot be null")
        @DecimalMin(value = "0.0", inclusive = true, message = "Basing price must be >= 0")
        BigDecimal basePrice,

        @NotNull(message = "Vehicle rule ID cannot be null")
        String sizeRuleId,

        @NotNull(message = "Distance rule ID cannot be null")
        String distanceRuleId
) {
}
