package capstone_project.entity.device;

import capstone_project.entity.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "device_types", schema = "public", catalog = "capstone-project")
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class DeviceTypeEntity extends BaseEntity {
    @Size(max = 100)
    @NotNull
    @Column(name = "device_type_name", nullable = false, length = 100)
    private String deviceTypeName;

    @Column(name = "vehicle_capacity", precision = 11, scale = 8)
    private BigDecimal vehicleCapacity;

    @Size(max = 200)
    @Column(name = "description", length = 200)
    private String description;

    @Column(name = "is_active")
    private Boolean isActive;

}