package capstone_project.dtos.response.device;

public record DeviceTypeResponse(
        String id,
        String deviceTypeName,
        String description,
        Boolean isActive
) {
}
