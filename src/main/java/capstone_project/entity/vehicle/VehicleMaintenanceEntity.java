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

    @Column(name = "cost", precision = 10, scale = 2)
    private BigDecimal cost;

    @Column(name = "next_maintenance_date")
    private LocalDateTime nextMaintenanceDate;

    @Column(name = "odometer_reading")
    private Integer odometerReading;

    @Size(max = 200)
    @Column(name = "service_center", length = 200)
    private String serviceCenter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    private VehicleEntity vehicleEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maintenance_type_id")
    private MaintenanceTypeEntity maintenanceTypeEntity;

}