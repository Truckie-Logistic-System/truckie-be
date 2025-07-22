package capstone_project.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "vehicle_types", schema = "public", catalog = "capstone-project")
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class VehicleTypeEntity extends BaseEntity {
    @Size(max = 50)
    @NotNull
    @Column(name = "vehicle_type_name", nullable = false, length = 50)
    private String vehicleTypeName;

    @Size(max = 200)
    @Column(name = "description", length = 200)
    private String description;

}