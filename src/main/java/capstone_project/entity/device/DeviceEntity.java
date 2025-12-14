package capstone_project.entity.device;

import capstone_project.entity.common.BaseEntity;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.entity.vehicle.VehicleEntity;
import capstone_project.entity.vehicle.VehicleAssignmentDeviceEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "devices", schema = "public", catalog = "capstone-project")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class DeviceEntity extends BaseEntity {
    @Size(max = 100)
    @Column(name = "device_code", length = 100)
    private String deviceCode;

    @Size(max = 100)
    @Column(name = "manufacturer", length = 100)
    private String manufacturer;

    @Size(max = 100)
    @Column(name = "model", length = 100)
    private String model;

    @Size(max = 20)
    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "installed_at")
    private LocalDateTime installedAt;

    @Size(max = 100)
    @Column(name = "ip_address", length = 100)
    private String ipAddress;

    @Size(max = 100)
    @Column(name = "firmware_version", length = 100)
    private String firmwareVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_type_id")
    @EqualsAndHashCode.Exclude
    private DeviceTypeEntity deviceTypeEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    @EqualsAndHashCode.Exclude
    private VehicleEntity vehicleEntity;

    @OneToMany(mappedBy = "device", fetch = FetchType.LAZY)
    @EqualsAndHashCode.Exclude
    private Set<VehicleAssignmentDeviceEntity> vehicleAssignmentDevices = new HashSet<>();
}