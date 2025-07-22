package capstone_project.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "penalty_history", schema = "public", catalog = "capstone-project")
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class PenaltyHistoryEntity extends BaseEntity {
    @Size(max = 100)
    @Column(name = "violation_type", length = 100)
    private String violationType;

    @Column(name = "violation_description", length = Integer.MAX_VALUE)
    private String violationDescription;

    @Column(name = "penalty_amount", precision = 10, scale = 2)
    private BigDecimal penaltyAmount;

    @Column(name = "penalty_date")
    private LocalDate penaltyDate;

    @Size(max = 255)
    @Column(name = "location")
    private String location;

    @Size(max = 100)
    @Column(name = "status", length = 100)
    private String status;

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    @Column(name = "dispute_reason", length = Integer.MAX_VALUE)
    private String disputeReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_by")
    private DriverEntity issueBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_assignment_id")
    private VehicleAssignmentEntity vehicleAssignmentEntity;

}