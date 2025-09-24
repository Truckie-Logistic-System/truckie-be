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
}
