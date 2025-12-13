package capstone_project.dtos.request.user;

import capstone_project.common.enums.LicenseClassEnum;
import capstone_project.common.enums.enumValidator.EnumValidator;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;

/**
 * Request DTO for admin to create a new driver account.
 * Password is NOT required - BE will generate a random temporary password.
 * Driver will be created with INACTIVE status and must complete onboarding on first login.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminCreateDriverRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    // User info
    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Phone number is required")
    private String phoneNumber;

    private Boolean gender;

    @NotBlank(message = "Date of birth is required")
    private String dateOfBirth;

    // Driver-specific info
    @NotBlank(message = "Identity number is required")
    private String identityNumber;

    @NotBlank(message = "Driver license number is required")
    private String driverLicenseNumber;

    @NotBlank(message = "Card serial number is required")
    private String cardSerialNumber;

    @NotBlank(message = "Place of issue is required")
    private String placeOfIssue;

    @NotBlank(message = "Date of issue is required")
    private String dateOfIssue;

    @NotBlank(message = "Date of expiry is required")
    private String dateOfExpiry;

    @NotBlank(message = "License class is required")
    @EnumValidator(enumClass = LicenseClassEnum.class, message = "Invalid license class")
    private String licenseClass;

    @NotBlank(message = "Date of passing is required")
    private String dateOfPassing;
}
