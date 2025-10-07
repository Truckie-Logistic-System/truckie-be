package capstone_project.entity.order.order;

import capstone_project.entity.common.BaseEntity;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_seals", schema = "public", catalog = "capstone-project")
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class OrderSealEntity extends BaseEntity {

    @Column(name = "seal_date")
    private LocalDateTime sealDate;

    @Column(name = "description")
    private String description;

    @Size(max = 255)
    @Column(name = "seal_attached_image")
    private String sealAttachedImage;

    @Column(name = "seal_removal_time")
    private LocalDateTime sealRemovalTime;

    @Size(max = 500)
    @Column(name = "seal_removal_reason")
    private String sealRemovalReason;

    @Size(max = 20)
    @Column(name = "status", length = 20)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_assignment_id")
    private VehicleAssignmentEntity vehicleAssignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seal_id")
    private SealEntity seal;

}
