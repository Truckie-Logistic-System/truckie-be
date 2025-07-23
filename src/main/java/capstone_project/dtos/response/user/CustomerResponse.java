package capstone_project.dtos.response.user;

import capstone_project.dtos.response.auth.UserResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class CustomerResponse {

    private String id;
    private String companyName;
    private String representativeName;
    private String representativePhone;
    private String businessLicenseNumber;
    private String businessAddress;
    private String status;

    private UserResponse userResponse;

}
