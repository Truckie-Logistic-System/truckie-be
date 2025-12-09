package capstone_project.dtos.response.auth;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
@SuperBuilder()
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = -5481422754378816270L;

    private String authToken;
    private String refreshToken;

    private UserResponse user;

    /**
     * Indicates if this is the driver's first login (status = INACTIVE).
     * If true, driver must complete onboarding before accessing the app.
     */
    private Boolean firstTimeLogin;

    /**
     * List of required actions for first-time login.
     * e.g., ["CHANGE_PASSWORD", "UPLOAD_FACE"]
     */
    private java.util.List<String> requiredActions;
}
