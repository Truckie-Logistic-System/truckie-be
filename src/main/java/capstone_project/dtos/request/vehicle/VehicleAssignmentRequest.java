package capstone_project.dtos.request.vehicle;

import capstone_project.common.enums.VehicleAssignmentEnum;
import capstone_project.common.enums.VehicleStatusEnum;
import capstone_project.common.enums.enumValidator.EnumValidator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.time.LocalDateTime;

public record VehicleAssignmentRequest(
        @NotBlank(message = "vehicleId is required") String vehicleId,

        @NotBlank(message = "driverId is required")  String driverId,

        String descripton,

        @NotBlank(message = "status is required")
        @EnumValidator(enumClass = VehicleAssignmentEnum.class, message = "Invalid status for vehicle assignment")
        String status
) {}