package capstone_project.dtos.response.vehicle;

import java.time.Instant;
import java.util.UUID;

public record VehicleAssignmentResponse(
        UUID id,
        UUID vehicleId,
        UUID driverId,
        Instant startDate,
        Instant endDate,
        String status
) {}