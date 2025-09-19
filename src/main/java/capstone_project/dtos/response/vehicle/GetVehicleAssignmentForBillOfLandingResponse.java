package capstone_project.dtos.response.vehicle;

import capstone_project.dtos.response.user.DriverResponse;

public record GetVehicleAssignmentForBillOfLandingResponse(
    String id,
    String description,
    String status,
    DriverResponse driver1,
    DriverResponse driver2,
    VehicleResponse vehicleEntity
) {
}
