package capstone_project.entity.order.contract;

import capstone_project.entity.auth.UserEntity;
import capstone_project.entity.common.BaseEntity;
import capstone_project.entity.order.order.OrderEntity;
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
    @Size(max = 100)
    @Column(name = "contract_name", length = 100)
    private String contractName;

    @Column(name = "effective_date")
    private LocalDateTime effectiveDate;

    @Column(name = "expiration_date")
    private LocalDateTime expirationDate;

    @Column(name = "signing_deadline")
    private LocalDateTime signingDeadline;

    @Column(name = "deposit_payment_deadline")
    private LocalDateTime depositPaymentDeadline;

    @Column(name = "full_payment_deadline")
    private LocalDateTime fullPaymentDeadline;

    @Column(name = "total_value")
    private BigDecimal totalValue;

    @Column(name = "adjusted_value")
    private BigDecimal adjustedValue;

    // Custom deposit percentage for this contract (overrides global contract_settings if set)
    // NULL means use global setting, otherwise use this value (0-100)
    @Column(name = "custom_deposit_percent", precision = 5, scale = 2)
    private BigDecimal customDepositPercent;

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
    @JoinColumn(name = "user_id")
    private UserEntity staff;
}