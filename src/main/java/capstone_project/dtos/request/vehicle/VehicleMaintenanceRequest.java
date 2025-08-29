package capstone_project.dtos.request.vehicle;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

public record VehicleMaintenanceRequest(
        @NotNull(message = "maintenanceDate is required") LocalDateTime maintenanceDate,
        @Size(max = 200) String description,
        @NotNull(message = "cost is required") BigDecimal cost,
        LocalDateTime nextMaintenanceDate,
        Integer odometerReading,
        @Size(max = 200) String serviceCenter,
        @NotBlank(message = "vehicleId is required") String vehicleId,
        @NotBlank(message = "maintenanceTypeId is required") String maintenanceTypeId
) {}