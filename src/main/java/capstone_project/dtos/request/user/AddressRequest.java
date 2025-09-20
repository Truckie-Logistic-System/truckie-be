package capstone_project.dtos.request.user;

public record AddressRequest(
        String street,
        String ward,
        String province,
        Boolean addressType,
        String customerId
) {
}
