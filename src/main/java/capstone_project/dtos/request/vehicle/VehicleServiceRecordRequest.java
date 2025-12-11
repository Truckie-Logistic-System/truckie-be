package capstone_project.dtos.request.vehicle;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record VehicleServiceRecordRequest(
        @NotBlank(message = "serviceType is required") String serviceType,
        String serviceStatus,
        LocalDateTime plannedDate,
        LocalDateTime actualDate,
        LocalDateTime nextServiceDate,
        @Size(max = 200) String description,
        Integer odometerReading,
        @Size(max = 500) String notes,
        @NotBlank(message = "vehicleId is required") String vehicleId
) {}
