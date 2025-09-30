package capstone_project.dtos.response.vehicle;

import capstone_project.dtos.response.user.GetDriverForOrderResponse;

import java.util.UUID;

public record VehicleAssignmentByJpaResponse(
        UUID id,
        String status,
        String trackingCod,
        VehicleResponse vehicleEntity,
        GetDriverForOrderResponse driver1,
        GetDriverForOrderResponse driver2
) {
}
