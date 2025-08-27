package capstone_project.dtos.request.vehicle;

import java.math.BigDecimal;

public record UpdateVehicleRequest(
        String model,
        String manufacturer,
        Integer year,
        BigDecimal capacity,
        String status,
        BigDecimal currentLatitude,
        BigDecimal currentLongitude,
        String vehicleTypeId
) {}