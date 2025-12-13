package capstone_project.dtos.request.device;

import jakarta.validation.constraints.NotBlank;

public record DeviceTypeRequest(

        @NotBlank(message = "Device type name is required")
        String deviceTypeName,
        String description
) {
}
