package capstone_project.dtos.response.order.contract;

import java.math.BigDecimal;

public record OrderDetailForPackingResponse(
        String id,
        BigDecimal weight,
        BigDecimal weightBaseUnit,
        String unit,
        String trackingCode
) {
}
