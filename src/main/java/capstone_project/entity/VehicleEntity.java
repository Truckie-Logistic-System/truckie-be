package capstone_project.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "vehicles", schema = "public", catalog = "capstone-project")
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class VehicleEntity extends BaseEntity {
    @Size(max = 20)
    @NotNull
    @Column(name = "license_plate_number", nullable = false, length = 20)
    private String licensePlateNumber;

    @Size(max = 50)
    @Column(name = "model", length = 50)
    private String model;

    @Size(max = 50)
    @Column(name = "manufacturer", length = 50)
    private String manufacturer;

    @Column(name = "year")
    private Integer year;

    @Column(name = "capacity")
    private BigDecimal capacity;

    @Size(max = 20)
    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "current_latitude", precision = 10, scale = 8)
    private BigDecimal currentLatitude;

    @Column(name = "current_longitude", precision = 10, scale = 8)
    private BigDecimal currentLongitude;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_type_id")
    private VehicleTypeEntity vehicleTypeEntity;

}