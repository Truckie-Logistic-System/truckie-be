package capstone_project.entity.setting;

import capstone_project.entity.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "contract_settings", schema = "public", catalog = "capstone-project")
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ContractSettingEntity extends BaseEntity {

    @Column(name = "deposit_percent")
    private BigDecimal depositPercent;

    @Column(name = "expired_deposit_date")
    private LocalDateTime expiredDepositDate;

    @Column(name = "insurance_rate")
    private BigDecimal insuranceRate;
}
