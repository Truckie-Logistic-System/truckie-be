package capstone_project.entity.pricing;

import capstone_project.entity.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "distance_rules", schema = "public", catalog = "capstone-project")
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class DistanceRuleEntity extends BaseEntity {
    @Column(name = "from_km", precision = 11, scale = 2)
    private BigDecimal fromKm;

    @Column(name = "to_km", precision = 11, scale = 2)
    private BigDecimal toKm;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "display_name", length = 100)
    private String displayName;

    @Column(name = "is_base_price", nullable = false)
    private Boolean isBasePrice;

    @Column(name = "status", length = 20, nullable = false)
    private String status;
}