package capstone_project.dtos.response.vehicle;

import capstone_project.dtos.response.user.DriverResponse;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record SampleVehicleAssignmentResponse(
        List<UUID> assignedDetails,
        Map<VehicleResponse, List<DriverResponse>> sampleVehicleAssignment
) {
}
