package capstone_project.dtos.request.vehicle;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record VehicleAssignmentRequest(
        @NotBlank(message = "vehicleId is required") String vehicleId,
        @NotBlank(message = "driverId is required")  String driverId,
        @NotNull(message = "startDate is required") Instant startDate,
        Instant endDate,
        String status
) {}