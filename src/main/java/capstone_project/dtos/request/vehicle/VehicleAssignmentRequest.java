package capstone_project.dtos.request.vehicle;

import jakarta.validation.constraints.NotBlank;


public record VehicleAssignmentRequest(
        @NotBlank(message = "vehicleId is required") String vehicleId,

        @NotBlank(message = "driverId_1 is required")  String driverId_1,
        @NotBlank(message = "driverId_2 is required")  String driverId_2,

        String description
) {}