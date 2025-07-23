package capstone_project.entity.order.contract;

import capstone_project.entity.order.order.OrderEntity;
import capstone_project.entity.pricing.PricingRuleEntity;
import capstone_project.entity.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "contracts", schema = "public", catalog = "capstone-project")
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ContractEntity extends BaseEntity {
    @Size(max = 50)
    @Column(name = "contract_name", length = 50)
    private String contractName;

    @Column(name = "effective_date")
    private LocalDateTime effectiveDate;

    @Column(name = "expiration_date")
    private LocalDateTime expirationDate;

    @Column(name = "total_value")
    private BigDecimal totalValue;

    @Column(name = "attach_file_url", length = Integer.MAX_VALUE)
    private String attachFileUrl;

    @Size(max = 100)
    @Column(name = "description", length = 100)
    private String description;

    @Size(max = 20)
    @Column(name = "status", length = 20)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private OrderEntity orderEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pricing_rule_id")
    private PricingRuleEntity pricingRuleEntity;

}