package capstone_project.dtos.response.route;

public record TollResponse(
        String name,
        String address,
        String type,
        Long amount
) {}
