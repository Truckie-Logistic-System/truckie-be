package capstone_project.entity.order.conformation;

import capstone_project.entity.common.BaseEntity;
import capstone_project.entity.device.DeviceEntity;
import capstone_project.entity.order.order.OrderDetailEntity;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "photo_completions", schema = "public", catalog = "capstone-project")
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class PhotoCompletionEntity extends BaseEntity {
    @Column(name = "image_url", length = Integer.MAX_VALUE)
    private String imageUrl;

    @Size(max = 100)
    @Column(name = "description", length = 100)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_assignment_id")
    private VehicleAssignmentEntity vehicleAssignmentEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    private DeviceEntity deviceEntity;

}