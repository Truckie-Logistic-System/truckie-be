package capstone_project.dtos.request.auth;

import capstone_project.common.enums.enumValidator.EnumValidator;
import capstone_project.common.enums.RoleTypeEnum;
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
    @EnumValidator(enumClass = RoleTypeEnum.class, message = "Invalid role type")
    private String roleName;

    private String description;
    private Boolean isActive;
}
