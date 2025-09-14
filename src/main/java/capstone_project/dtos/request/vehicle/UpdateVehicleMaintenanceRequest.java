package capstone_project.dtos.request.vehicle;

import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

public record UpdateVehicleMaintenanceRequest(
        LocalDateTime maintenanceDate,
        @Size(max = 200) String description,
        BigDecimal cost,
        LocalDateTime nextMaintenanceDate,
        Integer odometerReading,
        @Size(max = 200) String serviceCenter,
        String vehicleId,
        String maintenanceTypeId
) {}