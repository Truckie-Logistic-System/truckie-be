package capstone_project.dtos.request.user;

import java.time.LocalDateTime;

public record UpdateDriverRequest(
        String identityNumber,
        String driverLicenseNumber,
        String cardSerialNumber,
        String placeOfIssue,
        LocalDateTime dateOfIssue,
        LocalDateTime dateOfExpiry,
        String licenseClass,
        LocalDateTime dateOfPassing
) {
}
