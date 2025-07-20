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
public class RegisterCustomerRequest extends RegisterUserRequest {

    @NotBlank(message = "Company name is required")
    private String companyName;
    @NotBlank(message = "Representative name is required")
    private String representativeName;
    @NotBlank(message = "Representative phone is required")
    private String representativePhone;
    @NotBlank(message = "Business license number is required")
    private String businessLicenseNumber;
    @NotBlank(message = "Business address is required")
    private String businessAddress;
}
