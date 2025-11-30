package capstone_project.dtos.response.issue;

import capstone_project.dtos.response.order.contract.PriceCalculationResponse;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Response DTO for return shipping fee calculation
 */
public record ReturnShippingFeeResponse(
        UUID issueId,
        
        BigDecimal calculatedFee, // Giá cước được tính từ hệ thống
        
        BigDecimal adjustedFee, // Giá ưu đãi (nếu staff đã điều chỉnh)
        
        BigDecimal finalFee, // Giá cuối cùng (adjustedFee nếu có, không thì là calculatedFee)
        
        BigDecimal distanceKm, // Khoảng cách delivery → pickup
        
        PriceCalculationResponse priceDetails // Chi tiết tính giá (steps, breakdown)
) {
}
