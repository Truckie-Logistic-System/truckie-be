package capstone_project.entity.order.order;

import capstone_project.entity.common.BaseEntity;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "category_pricing_detail", schema = "public", catalog = "capstone-project")
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class CategoryPricingDetailEntity extends BaseEntity {

    @Column(name = "price_multiplier", precision = 11, scale = 8)
    private BigDecimal priceMultiplier;

    @Column(name = "extra_fee", precision = 15, scale = 2)
    private BigDecimal extraFee;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    @JsonBackReference
    private CategoryEntity category;
}
