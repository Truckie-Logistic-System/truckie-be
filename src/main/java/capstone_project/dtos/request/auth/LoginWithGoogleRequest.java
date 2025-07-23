package capstone_project.dtos.request.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class LoginWithGoogleRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = -2042698245177681370L;

    private String email;
}