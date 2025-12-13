package capstone_project.dtos.request.vehicle;

import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record UpdateVehicleServiceRecordRequest(
        String serviceType,
        String serviceStatus,
        LocalDateTime plannedDate,
        LocalDateTime actualDate,
        LocalDateTime nextServiceDate,
        @Size(max = 200) String description,
        Integer odometerReading,
        @Size(max = 500) String notes,
        String vehicleId
) {}
