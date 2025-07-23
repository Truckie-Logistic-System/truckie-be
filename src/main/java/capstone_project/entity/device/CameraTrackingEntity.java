package capstone_project.entity.device;

import capstone_project.entity.common.BaseEntity;
import capstone_project.entity.order.order.OrderDetailEntity;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "camera_trackings", schema = "public", catalog = "capstone-project")
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class CameraTrackingEntity extends BaseEntity {
    @Column(name = "video_url", length = Integer.MAX_VALUE)
    private String videoUrl;

    @Column(name = "tracking_at")
    private LocalDateTime trackingAt;

    @Size(max = 100)
    @Column(name = "status", length = 100)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_detail_id")
    private OrderDetailEntity orderDetail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_assignment_id")
    private VehicleAssignmentEntity vehicleAssignmentEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    private DeviceEntity deviceEntity;

}