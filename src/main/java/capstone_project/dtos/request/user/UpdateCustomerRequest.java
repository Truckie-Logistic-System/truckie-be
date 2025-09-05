package capstone_project.dtos.request.user;

public record UpdateCustomerRequest(
        String companyName,
        String representativeName,
        String representativePhone,
        String businessLicenseNumber,
        String businessAddress
) {
}
