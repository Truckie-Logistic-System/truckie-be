package capstone_project.entity.vehicle;

import capstone_project.entity.common.BaseEntity;
import capstone_project.entity.device.DeviceEntity;
import capstone_project.entity.user.driver.DriverEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "vehicle_assignments", schema = "public", catalog = "capstone-project")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class VehicleAssignmentEntity extends BaseEntity {
    @Size(max = 100)
    @Column(name = "tracking_code", length = 100)
    private String trackingCode;

    @Size(max = 100)
    @Column(name = "description", length = 100)
    private String description;

    @Size(max = 20)
    @Column(name = "status", length = 20)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    @EqualsAndHashCode.Exclude
    private VehicleEntity vehicleEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id_1")
    @EqualsAndHashCode.Exclude
    private DriverEntity driver1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id_2")
    @EqualsAndHashCode.Exclude
    private DriverEntity driver2;

    @OneToMany(mappedBy = "vehicleAssignment", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @EqualsAndHashCode.Exclude
    private Set<VehicleAssignmentDeviceEntity> vehicleAssignmentDevices = new HashSet<>();

    // Convenience method to get devices from intermediate entity
    public Set<DeviceEntity> getDevices() {
        if (vehicleAssignmentDevices == null || vehicleAssignmentDevices.isEmpty()) {
            return new HashSet<>();
        }
        return vehicleAssignmentDevices.stream()
                .map(VehicleAssignmentDeviceEntity::getDevice)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
    }
}