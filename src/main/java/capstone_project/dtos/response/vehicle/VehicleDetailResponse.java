package capstone_project.dtos.response.vehicle;

import java.util.UUID;

/**
 * Enhanced vehicle information with fuel consumption details for staff
 */
public record VehicleDetailResponse(
    UUID id,
    String model,
    String manufacturer,
    String licensePlateNumber,
    String status,
    String vehicleType
) {}
