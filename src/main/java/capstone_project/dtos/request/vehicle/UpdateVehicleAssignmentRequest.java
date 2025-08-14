package capstone_project.dtos.request.vehicle;

import java.time.Instant;

public record UpdateVehicleAssignmentRequest(
        String vehicleId,
        String driverId,
        Instant startDate,
        Instant endDate,
        String status
) {}