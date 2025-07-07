package capstone_project.controller.dtos.request;

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
public class RegisterRequest extends LoginRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 2834600384867483553L;

    private Boolean gender;
    private String dateOfBirth;
    private String imageUrl;
}