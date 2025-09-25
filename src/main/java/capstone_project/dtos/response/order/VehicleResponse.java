package capstone_project.dtos.response.order;

import java.util.UUID;

/**
 * Enhanced vehicle response with full information for staff
 */
public record VehicleResponse(
    UUID id,
    String manufacturer,
    String model,
    String licensePlateNumber,
    String vehicleType
) {}
