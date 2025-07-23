package capstone_project.dtos.response.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder()
@AllArgsConstructor
@NoArgsConstructor
public class RefreshTokenResponse {
    private String accessToken;
    private String refreshToken;
}
