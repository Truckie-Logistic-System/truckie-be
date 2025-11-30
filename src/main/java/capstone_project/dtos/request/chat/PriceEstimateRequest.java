package capstone_project.dtos.request.chat;

import java.math.BigDecimal;

public record PriceEstimateRequest(
        BigDecimal distance,
        BigDecimal weight,
        String vehicleType, // Optional
        String categoryName  // Optional, default to "Hàng thông thường"
) {
}
