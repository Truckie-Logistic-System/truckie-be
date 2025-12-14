package capstone_project.entity.vehicle;

import capstone_project.entity.device.DeviceEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "vehicle_assignment_devices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@IdClass(VehicleAssignmentDeviceEntity.VehicleAssignmentDeviceId.class)
public class VehicleAssignmentDeviceEntity {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_assignment_id", nullable = false)
    private VehicleAssignmentEntity vehicleAssignment;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private DeviceEntity device;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // Composite key class
    public static class VehicleAssignmentDeviceId implements Serializable {
        private UUID vehicleAssignment;
        private UUID device;

        public VehicleAssignmentDeviceId() {}

        public VehicleAssignmentDeviceId(UUID vehicleAssignment, UUID device) {
            this.vehicleAssignment = vehicleAssignment;
            this.device = device;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            VehicleAssignmentDeviceId that = (VehicleAssignmentDeviceId) o;
            return Objects.equals(vehicleAssignment, that.vehicleAssignment) &&
                   Objects.equals(device, that.device);
        }

        @Override
        public int hashCode() {
            return Objects.hash(vehicleAssignment, device);
        }
    }
}
