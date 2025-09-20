package capstone_project.dtos.response.vehicle;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record VehicleAssignmentResponse(
        UUID id,
        UUID vehicleId,
        UUID driver_id_1,
        UUID driver_id_2
){
    public String status() {
        return null;
    }
}