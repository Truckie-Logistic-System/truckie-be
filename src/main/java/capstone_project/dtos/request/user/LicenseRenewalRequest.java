package capstone_project.dtos.request.user;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LicenseRenewalRequest {

    @NotNull(message = "Ngày sát hạch không được để trống")
    private LocalDate dateOfPassing;

    @NotNull(message = "Ngày cấp không được để trống")
    private LocalDate dateOfIssue;

    @NotNull(message = "Ngày hết hạn không được để trống")
    private LocalDate dateOfExpiry;
}
