package capstone_project.dtos.response.order.contract;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Payment breakdown snapshot - stores all pricing details at contract creation time
 * This ensures historical accuracy even if system rates/settings change later
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentBreakdownSnapshot {

    // Price calculation details
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

    // Detailed calculation steps
    private List<CalculationStep> steps;      // Các bước tính chi tiết

    // Vehicle assignment details
    private List<VehicleAssignment> vehicleAssignments; // Chi tiết phân bổ xe

    // Distance information
    private BigDecimal distanceKm;            // Khoảng cách tính toán (km)

    // Deposit information
    private BigDecimal depositPercent;        // Tỷ lệ cọc (%) tại thời điểm tạo hợp đồng
    private BigDecimal depositAmount;         // Số tiền cọc
    private BigDecimal remainingAmount;       // Số tiền còn lại sau khi trừ cọc

    // Contract-specific adjustments
    private BigDecimal adjustedValue;         // Giá trị điều chỉnh bởi staff (giá ưu đãi)
    private BigDecimal effectiveTotal;        // Giá cuối cùng thực tế (adjustedValue nếu có, nếu không dùng grandTotal)

    // Snapshot metadata
    private String snapshotDate;              // Ngày tạo snapshot
    private String snapshotVersion;           // Phiên bản của pricing logic

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CalculationStep {
        private String sizeRuleName;          // Tên loại xe
        private int numOfVehicles;            // Số lượng xe
        private String distanceRange;         // Khoảng cách áp dụng
        private BigDecimal unitPrice;         // Đơn giá
        private BigDecimal appliedKm;         // Số km áp dụng
        private BigDecimal subtotal;          // Tạm tính
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class VehicleAssignment {
        private String vehicleId;             // ID xe (nếu có)
        private String vehicleTypeName;       // Tên loại xe
        private String licensePlate;          // Biển số xe (nếu có)
        private String sizeRuleName;          // Tên quy tắc kích thước
        private BigDecimal pricePerKm;        // Giá tiền trên km
        private Integer quantity;             // Số lượng xe
    }
}
