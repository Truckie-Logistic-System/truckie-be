package capstone_project.dtos.request.device;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record DeviceRequest(
        @NotBlank(message = "Device code must not be blank")
        String deviceCode,

        @NotBlank(message = "Manufacturer must not be blank")
        String manufacturer,

        @NotBlank(message = "Model must not be blank")
        String model,

        @JsonFormat(pattern = "yyyy-MM-dd")
        @NotNull(message = "Installed at must not be null")
        LocalDate installedAt,

        @NotBlank(message = "IP address must not be blank")
        String ipAddress,

        @NotBlank(message = "Firmware version must not be blank")
        String firmwareVersion,

        @NotBlank(message = "Device type ID must not be blank")
        String deviceTypeId,

        String vehicleId  // Optional: device may not be assigned to a vehicle yet
) {
}
