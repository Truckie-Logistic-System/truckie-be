package capstone_project.entity.order.order;

import capstone_project.entity.common.BaseEntity;
import capstone_project.entity.auth.UserEntity;
import capstone_project.entity.issue.IssueEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "refunds", schema = "public", catalog = "capstone-project")
@Data
@EqualsAndHashCode(callSuper = false)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class RefundEntity extends BaseEntity {
    @Column(name = "refund_amount", precision = 15, scale = 2)
    private BigDecimal refundAmount;

    @Size(max = 500)
    @Column(name = "bank_transfer_image", length = 500)
    private String bankTransferImage; // Ảnh chứng từ chuyển khoản

    @Size(max = 200)
    @Column(name = "bank_name", length = 200)
    private String bankName;

    @Size(max = 100)
    @Column(name = "account_number", length = 100)
    private String accountNumber;

    @Size(max = 200)
    @Column(name = "account_holder_name", length = 200)
    private String accountHolderName;

    @Size(max = 100)
    @Column(name = "transaction_code", length = 100)
    private String transactionCode;

    @Column(name = "refund_date")
    private LocalDateTime refundDate;

    @Size(max = 500)
    @Column(name = "notes", length = 500)
    private String notes;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id")
    private IssueEntity issueEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by_staff_id")
    private UserEntity processedByStaff; // Staff thực hiện refund
    
    // Source tracking for flexible refund origins
    @Size(max = 50)
    @Column(name = "source_type", length = 50)
    private String sourceType; // e.g., "ISSUE_DAMAGE", "ORDER_CANCELLATION", "OVERCHARGE"
    
    @Column(name = "source_id")
    private java.util.UUID sourceId; // Reference to source entity (issue_id, order_id, etc.)
}
