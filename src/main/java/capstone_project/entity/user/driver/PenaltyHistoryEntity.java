package capstone_project.entity.user.driver;

import capstone_project.entity.common.BaseEntity;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

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

    @Column(name = "penalty_date")
    private LocalDate penaltyDate;

    @Column(name = "traffic_violation_record_image_url", length = Integer.MAX_VALUE)
    private String trafficViolationRecordImageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private DriverEntity issueBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_assignment_id")
    private VehicleAssignmentEntity vehicleAssignmentEntity;

}