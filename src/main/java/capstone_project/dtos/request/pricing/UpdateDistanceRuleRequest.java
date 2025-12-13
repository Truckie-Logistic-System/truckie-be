package capstone_project.dtos.request.pricing;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdateDistanceRuleRequest(
        @NotNull(message = "Khoảng cách từ không được để trống")
        @DecimalMin(value = "0.0", message = "Khoảng cách từ phải >= 0")
        BigDecimal fromKm,

        @NotNull(message = "Khoảng cách đến không được để trống")
        @DecimalMin(value = "0.01", message = "Khoảng cách đến phải > 0")
        BigDecimal toKm
) {
}
