package capstone_project.dtos.response.vehicle;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record VehicleAssignmentResponse(
        UUID id,
        UUID vehicleId,
        UUID driverId_1,
        UUID driverId_2,
        String description,
        String status
) {}