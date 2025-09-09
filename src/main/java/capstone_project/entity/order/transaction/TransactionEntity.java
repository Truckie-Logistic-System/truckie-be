package capstone_project.entity.order.transaction;

import capstone_project.entity.common.BaseEntity;
import capstone_project.entity.order.contract.ContractEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transaction", schema = "public", catalog = "capstone-project")
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEntity extends BaseEntity {
    @Size(max = 50)
    @Column(name = "payment_provider", length = 50)
    private String paymentProvider;

    @Column(name = "amount")
    private BigDecimal amount;

    @Size(max = 10)
    @Column(name = "currency_code", length = 10)
    private String currencyCode;

    @Column(name = "gateway_response", length = Integer.MAX_VALUE)
    private String gatewayResponse;

    @Column(name = "gateway_order_code")
    private Long gatewayOrderCode;

    @Size(max = 20)
    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    private ContractEntity contractEntity;

}