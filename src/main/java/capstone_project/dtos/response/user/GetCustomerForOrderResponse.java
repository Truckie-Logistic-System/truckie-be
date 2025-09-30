package capstone_project.dtos.response.user;

import capstone_project.dtos.response.auth.UserResponse;

public record GetCustomerForOrderResponse(
        String id,
        String companyName,
        String representativeName,
        String representativePhone,
        String businessLicenseNumber,
        String businessAddress,
        String status,

        UserResponse user
) {
}
