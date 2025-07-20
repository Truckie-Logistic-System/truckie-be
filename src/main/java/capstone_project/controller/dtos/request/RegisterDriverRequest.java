package capstone_project.controller.dtos.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class RegisterDriverRequest extends RegisterUserRequest {

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
    private String licenseClass;
    @NotBlank(message = "Date of passing is required")
    private String dateOfPassing;
}
