package capstone_project.dtos.response.auth;

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
public class UserResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = -7290594370531933849L;

    private String id;
    private String username;
    private String fullName;
    private String email;
    private String phoneNumber;
    private Boolean gender;
    private String dateOfBirth;
    private String imageUrl;
    private String status;

    private RoleResponse role;
}
