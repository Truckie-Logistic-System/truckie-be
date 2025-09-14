package capstone_project.dtos.request.device;

import java.math.BigDecimal;

public record UpdateDeviceTypeRequest(
        String deviceTypeName,
        String description,
        Boolean isActive,
        BigDecimal vehicleCapacity
) {
}
