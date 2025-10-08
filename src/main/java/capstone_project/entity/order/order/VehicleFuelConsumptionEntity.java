package capstone_project.entity.order.order;

import capstone_project.entity.common.BaseEntity;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "vehicle_fuel_consumptions", schema = "public", catalog = "capstone-project")
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class VehicleFuelConsumptionEntity extends BaseEntity {

    @Column(name = "fuel_volume")
    private BigDecimal fuelVolume;

    @Column(name = "company_invoice_image_url", length = Integer.MAX_VALUE)
    private String companyInvoiceImageUrl;

    @Column(name = "odometer_at_start_url", length = Integer.MAX_VALUE)
    private String odometerAtStartUrl;

    @Column(name = "odometer_reading_at_start")
    private BigDecimal odometerReadingAtStart;

    @Column(name = "odometer_at_end_url", length = Integer.MAX_VALUE)
    private String odometerAtEndUrl;

    @Column(name = "odometer_reading_at_end")
    private BigDecimal odometerReadingAtEnd;

    @Column(name = "distance_traveled")
    private BigDecimal distanceTraveled;

    @Column(name = "date_recorded")
    private LocalDateTime dateRecorded;

    @Column(name = "notes", length = Integer.MAX_VALUE)
    private String notes;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_assignment_id", unique = true)
    private VehicleAssignmentEntity vehicleAssignmentEntity;
}
