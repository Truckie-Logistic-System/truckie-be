package capstone_project.dtos.response.setting;

import java.math.BigDecimal;

public record ContractSettingResponse(

        String id,
        BigDecimal depositPercent,
        Integer depositDeadlineHours,   // Hạn thanh toán cọc (số giờ)
        Integer signingDeadlineHours,   // Hạn ký hợp đồng (số giờ)
        Integer fullPaymentDaysBeforePickup,  // Số ngày trước khi lấy hàng phải thanh toán toàn bộ
        BigDecimal insuranceRateNormal,  // Tỷ lệ BH cho hàng thông thường (0.08%)
        BigDecimal insuranceRateFragile,  // Tỷ lệ BH cho hàng dễ vỡ (0.15%)
        BigDecimal vatRate
) {
}
