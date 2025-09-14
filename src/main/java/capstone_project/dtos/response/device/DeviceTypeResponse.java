package capstone_project.dtos.response.device;

import java.math.BigDecimal;

public record DeviceTypeResponse(
        String id,
        String deviceTypeName,
        BigDecimal vehicleCapacity,
        String description,
        Boolean isActive
) {
}
