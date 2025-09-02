package capstone_project.dtos.request.device;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record DeviceTypeRequest(

        @NotBlank(message = "Device type name is required")
        String deviceTypeName,
        BigDecimal vehicleCapacity,
        String description
) {
}
