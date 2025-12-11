package capstone_project.dtos.request.vehicle;

import capstone_project.common.enums.VehicleStatusEnum;
import capstone_project.common.enums.enumValidator.EnumValidator;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;

public record UpdateVehicleRequest(
        String model,
        String manufacturer,

        @Min(value = 1900, message = "Year must be valid")
        Integer year,

        @EnumValidator(enumClass = VehicleStatusEnum.class, message = "Invalid status for vehicle")
        String status,
        String licensePlateNumber,
        BigDecimal currentLatitude,
        BigDecimal currentLongitude,
        String vehicleTypeId
) {}