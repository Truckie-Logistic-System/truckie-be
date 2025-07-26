package capstone_project.dtos.request.vehicle;

import capstone_project.common.enums.VehicleTypeEnum;
import capstone_project.common.enums.enumValidator.EnumValidator;
import jakarta.validation.constraints.NotNull;

public record VehicleTypeRequest(
        @NotNull(message = "Vehicle type name is required")
        @EnumValidator(enumClass = VehicleTypeEnum.class, message = "Invalid vehicle type")
        String vehicleTypeName,
        @NotNull(message = "Description is required")
        String description
) {
}
