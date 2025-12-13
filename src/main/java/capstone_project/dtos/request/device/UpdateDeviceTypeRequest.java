package capstone_project.dtos.request.device;

public record UpdateDeviceTypeRequest(
        String deviceTypeName,
        String description,
        Boolean isActive
) {
}
