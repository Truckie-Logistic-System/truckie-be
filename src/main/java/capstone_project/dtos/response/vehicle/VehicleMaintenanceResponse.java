package capstone_project.dtos.response.vehicle;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record VehicleMaintenanceResponse(
        UUID id,
        Instant maintenanceDate,
        String description,
        BigDecimal cost,
        Instant nextMaintenanceDate,
        Integer odometerReading,
        String serviceCenter,
        UUID vehicleId,
        UUID maintenanceTypeId
) {}