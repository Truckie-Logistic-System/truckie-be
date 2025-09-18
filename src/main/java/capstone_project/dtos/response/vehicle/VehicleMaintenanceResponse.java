package capstone_project.dtos.response.vehicle;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record VehicleMaintenanceResponse(
        UUID id,
        LocalDateTime maintenanceDate,
        String description,
        BigDecimal cost,
        LocalDateTime nextMaintenanceDate,
        Integer odometerReading,
        String serviceCenter,
        VehicleResponse vehicleEntity,
        MaintenanceTypeResponse maintenanceTypeEntity
) {}