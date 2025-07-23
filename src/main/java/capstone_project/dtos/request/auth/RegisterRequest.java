package capstone_project.dtos.request.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest extends AuthBaseRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 2834600384867483553L;

    private Boolean gender;

    @NotBlank(message = "Date of birth is required")
    private String dateOfBirth;

    private String imageUrl;
}