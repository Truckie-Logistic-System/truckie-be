package capstone_project.dtos.request.device;

import capstone_project.common.enums.CommonStatusEnum;
import capstone_project.common.enums.enumValidator.EnumValidator;

import java.time.LocalDateTime;

public record UpdateDeviceRequest(
        String deviceCode,
        String manufacturer,
        String model,

        @EnumValidator(enumClass = CommonStatusEnum.class, message = "Status must be one of: ACTIVE, INACTIVE, DELETED")
        String status,
        String ipAddress,
        String firmwareVersion,
        LocalDateTime installedAt,

        String deviceTypeId,
        String vehicleId
) {
}
