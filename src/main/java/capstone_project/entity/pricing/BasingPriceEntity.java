package capstone_project.entity.pricing;

import capstone_project.entity.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "basing_prices", schema = "public", catalog = "capstone-project")
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class BasingPriceEntity extends BaseEntity {
    @Column(name = "base_price")
    private BigDecimal basePrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "size_rule_id")
    private SizeRuleEntity sizeRuleEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "distance_rule_id")
    private DistanceRuleEntity distanceRuleEntity;

}
