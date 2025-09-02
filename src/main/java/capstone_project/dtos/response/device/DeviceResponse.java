package capstone_project.dtos.response.device;

import java.time.LocalDateTime;

public record DeviceResponse(
        String id,
        String deviceCode,
        String manufacturer,
        String model,
        String status,
        LocalDateTime installedAt,
        String ipAddress,
        String firmwareVersion,

        String deviceTypeId,
        String vehicleId
        ) {
}
