package capstone_project.entity.order.order;

import capstone_project.common.enums.SealEnum;
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
@Table(name = "seals", schema = "public", catalog = "capstone-project")
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class SealEntity extends BaseEntity {

    @Column(name = "seal_code")
    private String sealCode;

    @Column(name = "seal_date")
    private LocalDateTime sealDate;

    @Column(name = "description")
    private String description;

    @Size(max = 255)
    @Column(name = "seal_attached_image")
    private String sealAttachedImage; // Ảnh seal khi được gắn lần đầu

    @Column(name = "status", length = 20)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_assignment_id")
    private VehicleAssignmentEntity vehicleAssignment;

}
