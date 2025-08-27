package capstone_project.dtos.request.vehicle;

import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public record UpdateVehicleMaintenanceRequest(
        Instant maintenanceDate,
        @Size(max = 200) String description,
        BigDecimal cost,
        Instant nextMaintenanceDate,
        Integer odometerReading,
        @Size(max = 200) String serviceCenter,
        String vehicleId,
        String maintenanceTypeId
) {}