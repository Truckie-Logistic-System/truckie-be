package capstone_project.controller.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Data
@SuperBuilder()
@AllArgsConstructor
@NoArgsConstructor
public class OTPResponse {
    private String otp;
    private LocalDateTime createdAt;

}
