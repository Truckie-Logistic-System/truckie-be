package capstone_project.controller.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.io.Serializable;

@Data
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class RoleResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = -5755886629553191665L;

    private String id;
    private String roleName;
    private String description;
    private Boolean isActive;
}
