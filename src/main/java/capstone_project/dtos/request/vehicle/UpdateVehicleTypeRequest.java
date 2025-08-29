package capstone_project.dtos.request.vehicle;

import capstone_project.common.enums.VehicleTypeEnum;
import capstone_project.common.enums.enumValidator.EnumValidator;
import jakarta.validation.constraints.NotBlank;

public record UpdateVehicleTypeRequest(
        @EnumValidator(enumClass = VehicleTypeEnum.class, message = "Invalid vehicle type")
        String vehicleTypeName,

        String description
) {
}
