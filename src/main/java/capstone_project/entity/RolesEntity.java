package capstone_project.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "roles", schema = "public", catalog = "capstone-project")
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class RolesEntity extends BaseEntity{

    @Basic
    @Column(name = "role_name")
    private String roleName;

    @Basic
    @Column(name = "description")
    private String description;

    @Basic
    @Column(name = "is_active")
    private Boolean isActive;
}