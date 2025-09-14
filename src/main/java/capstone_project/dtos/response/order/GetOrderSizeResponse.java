package capstone_project.dtos.response.order;

import java.math.BigDecimal;
import java.util.UUID;

public record GetOrderSizeResponse(
        UUID id,
        BigDecimal minWeight,
        BigDecimal maxWeight,
        BigDecimal minLength,
        BigDecimal maxLength,
        BigDecimal minHeight,
        BigDecimal maxHeight,
        BigDecimal minWidth,
        BigDecimal maxWidth,
        String status,
        String description
) {
}
