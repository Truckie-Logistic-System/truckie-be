package capstone_project.entity.pricing;

import capstone_project.entity.common.BaseEntity;
import capstone_project.entity.order.order.CategoryEntity;
import capstone_project.entity.vehicle.VehicleTypeEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "vehicle_type_rules", schema = "public", catalog = "capstone-project")
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class VehicleTypeRuleEntity extends BaseEntity {
    @Size(max = 100)
    @Column(name = "vehicle_type_rule_name", length = 100)
    private String vehicleTypeRuleName;

    @Column(name = "min_weight")
    private BigDecimal minWeight;

    @Column(name = "max_weight")
    private BigDecimal maxWeight;

    @Column(name = "min_length")
    private BigDecimal minLength;

    @Column(name = "max_length")
    private BigDecimal maxLength;

    @Column(name = "min_height")
    private BigDecimal minHeight;

    @Column(name = "max_height")
    private BigDecimal maxHeight;

    @Column(name = "min_width")
    private BigDecimal minWidth;

    @Column(name = "max_width")
    private BigDecimal maxWidth;

    @Column(name = "effective_from")
    private LocalDateTime effectiveFrom;

    @Column(name = "effective_to")
    private LocalDateTime effectiveTo;

    @Size(max = 20)
    @Column(name = "status", length = 20)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private CategoryEntity category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_type_id")
    private VehicleTypeEntity vehicleTypeEntity;
}