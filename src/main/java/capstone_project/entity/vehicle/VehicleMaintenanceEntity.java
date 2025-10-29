package capstone_project.entity.vehicle;

import capstone_project.entity.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "vehicle_maintenance", schema = "public", catalog = "capstone-project")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class VehicleMaintenanceEntity extends BaseEntity {
    @Column(name = "maintenance_date")
    private LocalDateTime maintenanceDate;

    @Size(max = 200)
    @Column(name = "description", length = 200)
    private String description;

    @Column(name = "next_maintenance_date")
    private LocalDateTime nextMaintenanceDate;

    @Column(name = "expire_maintenance_date")
    private LocalDateTime expiredMaintenanceDate;

    @Column(name = "odometer_reading")
    private Integer odometerReading;

    @Size(max = 20)
    @Column(name = "status", length = 20)
    private String Status;

    @Column(name = "request_at")
    private LocalDateTime requestAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    @Column(name ="failure_reason", length = 200)
    private String failureReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    private VehicleEntity vehicleEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maintenance_type_id")
    private MaintenanceTypeEntity maintenanceTypeEntity;

}