package capstone_project.dtos.response.vehicle;

import capstone_project.entity.vehicle.VehicleEntity;

import java.util.List;
import java.util.UUID;

public record VehicleAssignmentPair(
        List<VehicleEntity> vehicles, List<UUID> assignedDetails
) {
}
