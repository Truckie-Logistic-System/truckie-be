package capstone_project.entity.setting;

import capstone_project.entity.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "contract_settings", schema = "public", catalog = "capstone-project")
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class ContractSettingEntity extends BaseEntity {

    @Column(name = "deposit_percent")
    private BigDecimal depositPercent;

    // Hạn thanh toán cọc (số giờ) - mặc định 24 giờ
    @Column(name = "deposit_deadline_hours")
    private Integer depositDeadlineHours;

    // Hạn ký hợp đồng (số giờ) - mặc định 24 giờ
    @Column(name = "signing_deadline_hours")
    private Integer signingDeadlineHours;

    // Số ngày trước khi lấy hàng phải thanh toán toàn bộ - mặc định 1 ngày
    @Column(name = "full_payment_days_before_pickup")
    private Integer fullPaymentDaysBeforePickup;

    // Tỷ lệ bảo hiểm cho hàng thông thường (0.0008 = 0.08%)
    @Column(name = "insurance_rate_normal", precision = 6, scale = 5)
    private BigDecimal insuranceRateNormal;

    // Tỷ lệ bảo hiểm cho hàng dễ vỡ/rủi ro cao (0.0015 = 0.15%)
    @Column(name = "insurance_rate_fragile", precision = 6, scale = 5)
    private BigDecimal insuranceRateFragile;

    @Column(name = "vat_rate")
    private BigDecimal vatRate;
}
