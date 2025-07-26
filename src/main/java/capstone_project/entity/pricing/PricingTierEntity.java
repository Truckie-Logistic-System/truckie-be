package capstone_project.entity.pricing;

import capstone_project.entity.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "pricing_tiers", schema = "public", catalog = "capstone-project")
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class PricingTierEntity extends BaseEntity {
    @Column(name = "base_price")
    private BigDecimal basePrice;

    @Size(max = 100)
    @Column(name = "description", length = 100)
    private String description;

    @Column(name = "from_km", precision = 11, scale = 8)
    private BigDecimal fromKm;

    @Column(name = "to_km", precision = 11, scale = 8)
    private BigDecimal toKm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pricing_rule_id")
    private PricingRuleEntity pricingRuleEntity;

}