package capstone_project.dtos.response.order.contract;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PriceCalculationResponse {

    private BigDecimal totalPrice;            // Tổng cuối cùng (giữ để tương thích cũ)
    private BigDecimal totalBeforeAdjustment; // Tổng trước khi nhân hệ số & phụ phí
    private BigDecimal categoryExtraFee;      // Phụ phí loại hàng
    private BigDecimal categoryMultiplier;    // Hệ số loại hàng
    private BigDecimal promotionDiscount;     // Giảm giá khuyến mãi (nếu có)
    private BigDecimal finalTotal;            // Tổng cuối cùng sau mọi điều chỉnh

    private List<CalculationStep> steps;      // Các bước tính chi tiết
//    private String summary;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CalculationStep {
        private String sizeRuleName;
        private int numOfVehicles;
        private String distanceRange;
        private BigDecimal unitPrice;
        private BigDecimal appliedKm;
        private BigDecimal subtotal;
    }
}
