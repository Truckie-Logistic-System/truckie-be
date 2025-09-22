package capstone_project.dtos.response.user;

import capstone_project.dtos.response.auth.UserResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

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
    private String dateOfIssue;
    private String dateOfExpiry;
    private String licenseClass;
    private String dateOfPassing;
    private String status;

    private UserResponse userResponse;

    private List<PenaltyHistoryResponse> penaltyHistories;
}
