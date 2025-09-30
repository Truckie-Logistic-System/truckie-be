package capstone_project.dtos.response.vehicle;

import java.math.BigDecimal;

public record VehicleForOrderResponse(
        String id,
        String licensePlateNumber,
        String model,
        String manufacturer,
        Integer year,
        BigDecimal capacity,
        String status,
        VehicleTypeResponse vehicleTypeEntity
) {
}
