package capstone_project.dtos.request.vehicle;

import capstone_project.common.enums.VehicleStatusEnum;
import capstone_project.common.enums.enumValidator.EnumValidator;
import jakarta.validation.constraints.*;

public record VehicleRequest(
        @NotBlank(message = "License plate number is required")
        String licensePlateNumber,

        @NotBlank(message = "Model is required")
        String model,

        @NotBlank(message = "Manufacturer is required")
        String manufacturer,

        @NotNull(message = "Year is required")
        @Min(value = 1900, message = "Year must be valid")
        Integer year,

        @NotBlank(message = "Status is required")
        @EnumValidator(enumClass = VehicleStatusEnum.class, message = "Invalid status for vehicle")
        String status,

        @NotBlank(message = "Vehicle type ID is required")
        String vehicleTypeId
) {}