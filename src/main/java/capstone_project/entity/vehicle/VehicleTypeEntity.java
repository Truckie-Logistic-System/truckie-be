package capstone_project.entity.vehicle;

import capstone_project.entity.common.BaseEntity;
import capstone_project.entity.order.order.FuelTypeEntity;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "vehicle_types", schema = "public", catalog = "capstone-project")
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class VehicleTypeEntity extends BaseEntity {
    @Size(max = 100)
    @NotNull
    @Column(name = "vehicle_type_name", nullable = false, length = 100)
    private String vehicleTypeName;

    @Size(max = 200)
    @Column(name = "description", length = 200)
    private String description;

    @Column(name = "capacity_m3")
    private BigDecimal capacityM3;

    @Column(name = "weight_limit_ton")
    private BigDecimal weightLimitTon;

    @Column(name = "average_fuel_consumption_l_per_100km")
    private BigDecimal averageFuelConsumptionLPer100km;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fuel_type_id")
    private FuelTypeEntity fuelTypeEntity;

}