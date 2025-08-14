package capstone_project.dtos.request.vehicle;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

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

        @NotNull(message = "Capacity is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Capacity must be >= 0")
        BigDecimal capacity,

        String status,

        @NotBlank(message = "Vehicle type ID is required")
        String vehicleTypeId
) {}