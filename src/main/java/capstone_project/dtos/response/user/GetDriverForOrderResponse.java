package capstone_project.dtos.response.user;

import capstone_project.dtos.response.auth.UserResponse;

public record GetDriverForOrderResponse(
        String id,
        String identityNumber,
        String driverLicenseNumber,
        String cardSerialNumber,
        String placeOfIssue,
        String dateOfIssue,
        String dateOfExpiry,
        String licenseClass,
        String dateOfPassing,
        String status,

        UserResponse user
) {
}
