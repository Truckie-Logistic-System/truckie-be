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
    
    // Toll fee information
    private BigDecimal totalTollFee;          // Tổng phí cầu đường
    private Integer totalTollCount;           // Số lượng trạm thu phí
    private String vehicleType;               // Loại phương tiện sử dụng để tính phí
    
    // Insurance fields
    private BigDecimal totalDeclaredValue;    // Tổng giá trị khai báo
    private BigDecimal insuranceFee;          // Phí bảo hiểm (đã bao gồm VAT)
    private BigDecimal insuranceRate;         // Tỷ lệ bảo hiểm (0.08% hoặc 0.15%)
    private BigDecimal vatRate;               // Tỷ lệ VAT (10%)
    private Boolean hasInsurance;             // Có mua bảo hiểm không
    private BigDecimal grandTotal;            // Tổng cuối cùng (bao gồm cả phí bảo hiểm và phí cầu đường)

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
