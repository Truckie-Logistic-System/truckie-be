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
import java.util.UUID;

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

    @Size(max = 255) // Adding a reasonable size constraint
    @Column(name = "gateway_order_code")
    private String gatewayOrderCode;

    @Size(max = 20)
    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    @Size(max = 20)
    @Column(name = "transaction_type", length = 20)
    private String transactionType; // DEPOSIT, FULL_PAYMENT, RETURN_SHIPPING

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    private ContractEntity contractEntity;

    @Column(name = "issue_id")
    private UUID issueId; // For RETURN_SHIPPING transactions

}