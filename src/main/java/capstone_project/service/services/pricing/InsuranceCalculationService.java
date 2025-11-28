package capstone_project.service.services.pricing;

import capstone_project.common.enums.CategoryName;
import capstone_project.entity.order.order.OrderDetailEntity;
import capstone_project.entity.order.order.OrderEntity;
import capstone_project.entity.setting.ContractSettingEntity;
import capstone_project.repository.entityServices.setting.ContractSettingEntityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Service tính phí bảo hiểm hàng hóa
 * 
 * Công thức: Phí BH = Giá trị Khai báo × Tỷ lệ BH × (1 + VAT)
 * 
 * Tỷ lệ BH:
 * - Hàng thông thường: 0.08% (0.0008)
 * - Hàng dễ vỡ/rủi ro cao: 0.15% (0.0015)
 * 
 * VAT: 10% (0.10)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InsuranceCalculationService {

    private final ContractSettingEntityService contractSettingEntityService;

    // Default values if not configured
    private static final BigDecimal DEFAULT_INSURANCE_RATE_NORMAL = new BigDecimal("0.0008");  // 0.08%
    private static final BigDecimal DEFAULT_INSURANCE_RATE_FRAGILE = new BigDecimal("0.0015"); // 0.15%
    private static final BigDecimal DEFAULT_VAT_RATE = new BigDecimal("0.10"); // 10%

    /**
     * Lấy cấu hình contract settings hiện tại
     */
    private ContractSettingEntity getContractSettings() {
        try {
            return contractSettingEntityService.findFirstByOrderByCreatedAtAsc()
                    .orElse(null);
        } catch (Exception e) {
            log.warn("⚠️ Could not load contract settings, using defaults: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Kiểm tra loại hàng có phải là hàng dễ vỡ/rủi ro cao không
     * Sử dụng enum CategoryName để kiểm tra chính xác
     */
    public boolean isFragileCategory(CategoryName categoryName) {
        return categoryName == CategoryName.FRAGILE;
    }

    /**
     * Lấy tỷ lệ bảo hiểm theo loại hàng
     * 
     * Note: DB stores rate as percentage value (e.g., 0.15 = 0.15%)
     * This method returns the decimal rate for calculation (e.g., 0.0015)
     */
    public BigDecimal getInsuranceRate(boolean isFragile) {
        ContractSettingEntity settings = getContractSettings();
        
        if (settings != null) {
            BigDecimal rate = isFragile ? 
                    settings.getInsuranceRateFragile() : 
                    settings.getInsuranceRateNormal();
            if (rate != null) {
                // DB stores percentage value (0.15 = 0.15%), convert to decimal (0.0015)
                // Only convert if rate >= 0.01 (assuming DB stores as percentage)
                if (rate.compareTo(new BigDecimal("0.01")) >= 0) {
                    return rate.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP);
                }
                return rate; // Already in decimal format
            }
        }
        
        return isFragile ? DEFAULT_INSURANCE_RATE_FRAGILE : DEFAULT_INSURANCE_RATE_NORMAL;
    }
    
    /**
     * Lấy tỷ lệ bảo hiểm để hiển thị (dạng phần trăm, e.g., 0.15 = 0.15%)
     */
    public BigDecimal getInsuranceRateForDisplay(boolean isFragile) {
        ContractSettingEntity settings = getContractSettings();
        
        if (settings != null) {
            BigDecimal rate = isFragile ? 
                    settings.getInsuranceRateFragile() : 
                    settings.getInsuranceRateNormal();
            if (rate != null) {
                // If rate is already in decimal format (< 0.01), convert to percentage
                if (rate.compareTo(new BigDecimal("0.01")) < 0) {
                    return rate.multiply(new BigDecimal("100"));
                }
                return rate; // Already in percentage format
            }
        }
        
        // Convert defaults to percentage for display
        BigDecimal defaultRate = isFragile ? DEFAULT_INSURANCE_RATE_FRAGILE : DEFAULT_INSURANCE_RATE_NORMAL;
        return defaultRate.multiply(new BigDecimal("100"));
    }

    /**
     * Lấy tỷ lệ VAT
     */
    public BigDecimal getVatRate() {
        ContractSettingEntity settings = getContractSettings();
        if (settings != null && settings.getVatRate() != null) {
            return settings.getVatRate();
        }
        return DEFAULT_VAT_RATE;
    }

    /**
     * Tính phí bảo hiểm cho 1 kiện hàng (chưa có VAT)
     */
    public BigDecimal calculateInsuranceFeeWithoutVat(BigDecimal declaredValue, boolean isFragile) {
        if (declaredValue == null || declaredValue.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal rate = getInsuranceRate(isFragile);
        return declaredValue.multiply(rate).setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * Tính phí bảo hiểm cho 1 kiện hàng (đã bao gồm VAT)
     * 
     * @param declaredValue Giá trị khai báo
     * @param isFragile Là hàng dễ vỡ/rủi ro cao
     * @return Phí BH đã bao gồm VAT, làm tròn đến đơn vị VNĐ
     */
    public BigDecimal calculateInsuranceFeeWithVat(BigDecimal declaredValue, boolean isFragile) {
        if (declaredValue == null || declaredValue.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal rate = getInsuranceRate(isFragile);
        BigDecimal vatRate = getVatRate();
        
        // Phí BH = declaredValue × rate × (1 + VAT)
        BigDecimal baseFee = declaredValue.multiply(rate);
        BigDecimal totalFee = baseFee.multiply(BigDecimal.ONE.add(vatRate));
        
        return totalFee.setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * Tính tổng phí bảo hiểm cho tất cả kiện hàng
     * 
     * @param orderDetails Danh sách các kiện hàng
     * @param categoryName Tên loại hàng (enum NORMAL/FRAGILE)
     * @return Tổng phí BH đã bao gồm VAT
     */
    public BigDecimal calculateTotalInsuranceFee(List<OrderDetailEntity> orderDetails, CategoryName categoryName) {
        if (orderDetails == null || orderDetails.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        boolean isFragile = isFragileCategory(categoryName);
        
        return orderDetails.stream()
                .map(od -> calculateInsuranceFeeWithVat(od.getDeclaredValue(), isFragile))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Tính tổng giá trị khai báo của tất cả kiện hàng
     */
    public BigDecimal calculateTotalDeclaredValue(List<OrderDetailEntity> orderDetails) {
        if (orderDetails == null || orderDetails.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        return orderDetails.stream()
                .map(od -> od.getDeclaredValue() != null ? od.getDeclaredValue() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Tính và cập nhật phí bảo hiểm cho đơn hàng
     * 
     * @param order Đơn hàng cần cập nhật
     * @param categoryName Tên loại hàng (enum NORMAL/FRAGILE)
     */
    public void updateOrderInsurance(OrderEntity order, CategoryName categoryName) {
        if (order == null) return;
        
        List<OrderDetailEntity> orderDetails = order.getOrderDetailEntities();
        log.info("🔍 DEBUG: updateOrderInsurance called with {} orderDetails", orderDetails != null ? orderDetails.size() : 0);
        
        if (orderDetails != null && !orderDetails.isEmpty()) {
            orderDetails.forEach(od -> log.info("🔍 DEBUG: OrderDetail declaredValue: {}", od.getDeclaredValue()));
        }
        
        // Tính tổng giá trị khai báo
        BigDecimal totalDeclaredValue = calculateTotalDeclaredValue(orderDetails);
        order.setTotalDeclaredValue(totalDeclaredValue);
        log.info("🔍 DEBUG: Calculated totalDeclaredValue: {}", totalDeclaredValue);
        
        // Nếu có mua bảo hiểm, tính phí
        if (Boolean.TRUE.equals(order.getHasInsurance())) {
            BigDecimal totalInsuranceFee = calculateTotalInsuranceFee(orderDetails, categoryName);
            order.setTotalInsuranceFee(totalInsuranceFee);
            
            log.info("✅ Calculated insurance for order: totalDeclaredValue={}, totalInsuranceFee={}, isFragile={}",
                    totalDeclaredValue, totalInsuranceFee, isFragileCategory(categoryName));
        } else {
            order.setTotalInsuranceFee(BigDecimal.ZERO);
            log.info("ℹ️ Order does not have insurance: totalDeclaredValue={}", totalDeclaredValue);
        }
    }

    /**
     * Tính phí bảo hiểm dự kiến (dùng cho preview/estimate)
     * 
     * @param totalDeclaredValue Tổng giá trị khai báo
     * @param categoryName Tên loại hàng (enum NORMAL/FRAGILE)
     * @return Object chứa thông tin phí BH
     */
    public InsuranceEstimate estimateInsuranceFee(BigDecimal totalDeclaredValue, CategoryName categoryName) {
        boolean isFragile = isFragileCategory(categoryName);
        BigDecimal rate = getInsuranceRate(isFragile);
        BigDecimal vatRate = getVatRate();
        
        BigDecimal feeWithoutVat = totalDeclaredValue.multiply(rate).setScale(0, RoundingMode.HALF_UP);
        BigDecimal vatAmount = feeWithoutVat.multiply(vatRate).setScale(0, RoundingMode.HALF_UP);
        BigDecimal feeWithVat = feeWithoutVat.add(vatAmount);
        
        return new InsuranceEstimate(
                totalDeclaredValue,
                rate,
                vatRate,
                feeWithoutVat,
                vatAmount,
                feeWithVat,
                isFragile
        );
    }

    /**
     * Record chứa thông tin estimate phí bảo hiểm
     */
    public record InsuranceEstimate(
            BigDecimal totalDeclaredValue,
            BigDecimal insuranceRate,
            BigDecimal vatRate,
            BigDecimal feeWithoutVat,
            BigDecimal vatAmount,
            BigDecimal feeWithVat,
            boolean isFragile
    ) {}
}
