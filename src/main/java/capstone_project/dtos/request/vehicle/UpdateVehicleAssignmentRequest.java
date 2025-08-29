package capstone_project.dtos.request.vehicle;

import java.time.Instant;
import java.time.LocalDateTime;

public record UpdateVehicleAssignmentRequest(
        String vehicleId,
        String driverId,
        String description,
        String status
) {}