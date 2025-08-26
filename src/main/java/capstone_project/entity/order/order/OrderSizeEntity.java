package capstone_project.entity.order.order;

import capstone_project.entity.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "order_sizes", schema = "public", catalog = "capstone-project")
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class OrderSizeEntity extends BaseEntity {

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

    @Size(max = 20)
    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "description")
    @Size(max = 200)
    private String description;
}
