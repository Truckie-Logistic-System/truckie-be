package capstone_project.controller.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.sql.Timestamp;

@Data
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class DriverResponse {

    private String id;
    private String identityNumber;
    private String driverLicenseNumber;
    private String cardSerialNumber;
    private String placeOfIssue;
    private Timestamp dateOfIssue;
    private Timestamp dateOfExpiry;
    private String licenseClass;
    private Timestamp dateOfPassing;
    private String status;

    private UserResponse userResponse;
}
