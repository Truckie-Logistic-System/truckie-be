package capstone_project.entity.order.order;

import capstone_project.entity.common.BaseEntity;
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

    @Column(name = "fuel_cost")
    private BigDecimal fuelCost;

    @Column(name = "fuel_unit_price")
    private BigDecimal fuelUnitPrice;

    @Column(name = "odometer_reading_at_refuel")
    private BigDecimal odometerReadingAtRefuel;

    @Column(name = "date_recorded")
    private LocalDateTime dateRecorded;

    @Column(name = "notes", length = Integer.MAX_VALUE)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fuel_type_id")
    private FuelTypeEntity fuelTypeEntity;

}
