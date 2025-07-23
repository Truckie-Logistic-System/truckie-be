package capstone_project.entity.vehicle;

import capstone_project.entity.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "maintenance_types", schema = "public", catalog = "capstone-project")
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceTypeEntity extends BaseEntity {
    @Size(max = 50)
    @NotNull
    @Column(name = "maintenance_type_name", nullable = false, length = 50)
    private String maintenanceTypeName;

    @Size(max = 200)
    @Column(name = "description", length = 200)
    private String description;

    @Column(name = "is_active")
    private Boolean isActive;

}