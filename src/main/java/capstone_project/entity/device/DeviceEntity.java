package capstone_project.entity.device;

import capstone_project.entity.common.BaseEntity;
import capstone_project.entity.vehicle.VehicleEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "devices", schema = "public", catalog = "capstone-project")
@Data
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

    @Size(max = 45)
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Size(max = 50)
    @Column(name = "firmware_version", length = 50)
    private String firmwareVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_type_id")
    private DeviceTypeEntity deviceTypeEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    private VehicleEntity vehicleEntity;

}