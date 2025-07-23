package capstone_project.entity.vehicle;

import capstone_project.entity.common.BaseEntity;
import capstone_project.entity.order.order.OrderDetailEntity;
import capstone_project.entity.user.driver.DriverEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "vehicle_assignments", schema = "public", catalog = "capstone-project")
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class VehicleAssignmentEntity extends BaseEntity {
    @Size(max = 100)
    @Column(name = "description", length = 100)
    private String description;

    @Size(max = 20)
    @Column(name = "status", length = 20)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    private VehicleEntity vehicleEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private DriverEntity driver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_detail_id")
    private OrderDetailEntity orderDetail;

}