package capstone_project.controller.dtos.request;

import capstone_project.enums.EnumValidator;
import capstone_project.enums.RoleType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class RoleRequest {

    @NotNull(message = "Role name is required")
    @EnumValidator(enumClass = RoleType.class, message = "Invalid role type")
    private String roleName;

    private String description;
    private Boolean isActive;
}
