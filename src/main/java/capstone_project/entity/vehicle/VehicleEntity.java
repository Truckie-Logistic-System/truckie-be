package capstone_project.entity.vehicle;

import capstone_project.entity.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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

    @Size(max = 20)
    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "current_latitude", precision = 11, scale = 8)
    private BigDecimal currentLatitude;

    @Column(name = "current_longitude", precision = 11, scale = 8)
    private BigDecimal currentLongitude;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_type_id")
    private VehicleTypeEntity vehicleTypeEntity;

    // ========== Đăng kiểm (Inspection) ==========

    @Column(name = "last_inspection_date")
    private LocalDate lastInspectionDate;

    @Column(name = "inspection_expiry_date")
    private LocalDate inspectionExpiryDate;

    // ========== Bảo hiểm (Insurance) ==========

    @Column(name = "insurance_expiry_date")
    private LocalDate insuranceExpiryDate;

    @Size(max = 50)
    @Column(name = "insurance_policy_number", length = 50)
    private String insurancePolicyNumber;

    // ========== Bảo trì (Maintenance) ==========

    @Column(name = "last_maintenance_date")
    private LocalDate lastMaintenanceDate;

    @Column(name = "next_maintenance_date")
    private LocalDate nextMaintenanceDate;

}