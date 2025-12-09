package capstone_project.dtos.response.user;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class DriverSummary {

    // User info
    private UUID userId;
    private String username;
    private String fullName;
    private String email;
    private String phoneNumber;
    private Boolean gender;
    private LocalDate dateOfBirth;
    private String imageUrl;
    private String userStatus;
    private String roleName;

    // Driver info
    private UUID driverId;
    private String identityNumber;
    private String driverLicenseNumber;
    private String cardSerialNumber;
    private String placeOfIssue;
    private LocalDateTime dateOfIssue;
    private LocalDateTime dateOfExpiry;
    private String licenseClass;
    private LocalDateTime dateOfPassing;
    private String driverStatus;
}
