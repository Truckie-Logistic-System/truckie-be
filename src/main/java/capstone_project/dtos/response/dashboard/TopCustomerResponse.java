package capstone_project.dtos.response.dashboard;

public record TopCustomerResponse(
        String customerId,
        String companyName,
        Long orderCount

) {
}
