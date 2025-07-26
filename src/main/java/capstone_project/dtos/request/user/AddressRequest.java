package capstone_project.dtos.request.user;

public record AddressRequest(
        String province,
        String ward,
        String street,
        String addressType,
        String latitude,
        String longitude,
        String customerId
) {
}
