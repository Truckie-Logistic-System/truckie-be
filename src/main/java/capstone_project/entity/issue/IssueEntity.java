package capstone_project.entity.issue;

import capstone_project.entity.common.BaseEntity;
import capstone_project.entity.auth.UserEntity;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.entity.order.order.SealEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "issues", schema = "public", catalog = "capstone-project")
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class    IssueEntity extends BaseEntity {
    @Size(max = 200)
    @Column(name = "description", length = 200)
    private String description;

    @Column(name = "location_latitude", precision = 11, scale = 8)
    private BigDecimal locationLatitude;

    @Column(name = "location_longitude", precision = 11, scale = 8)
    private BigDecimal locationLongitude;

    @Size(max = 20)
    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "reported_at")
    private LocalDateTime reportedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Size(max = 20)
    @Column(name = "trip_status_at_report", length = 20)
    private String tripStatusAtReport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_assignment_id")
    private VehicleAssignmentEntity vehicleAssignmentEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_type_id")
    private IssueTypeEntity issueTypeEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity staff;

    // Seal replacement fields
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "old_seal_id")
    private SealEntity oldSeal; // Seal bị gỡ

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "new_seal_id")
    private SealEntity newSeal; // Seal thay thế

    @Size(max = 500)
    @Column(name = "seal_removal_image", length = 500)
    private String sealRemovalImage; // Ảnh seal cũ bị gỡ

    @Size(max = 500)
    @Column(name = "new_seal_attached_image", length = 500)
    private String newSealAttachedImage; // Ảnh seal mới được gắn

    @Column(name = "new_seal_confirmed_at")
    private LocalDateTime newSealConfirmedAt; // Thời gian driver xác nhận gắn seal mới

}