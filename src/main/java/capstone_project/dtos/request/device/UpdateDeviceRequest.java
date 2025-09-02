package capstone_project.dtos.request.device;

import java.time.LocalDateTime;

public record UpdateDeviceRequest(
        String deviceCode,
        String manufacturer,
        String model,
        String status,
        String ipAddress,
        String firmwareVersion,
        LocalDateTime installedAt,

        String deviceTypeId,
        String vehicleId
) {
}
