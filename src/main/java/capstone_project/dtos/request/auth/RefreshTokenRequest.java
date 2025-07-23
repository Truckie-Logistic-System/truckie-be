package capstone_project.dtos.request.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenRequest {
    @NotBlank
    private String refreshToken;
}
