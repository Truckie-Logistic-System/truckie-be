package capstone_project.dtos.response.vehicle;

import java.util.List;
import java.util.UUID;

/**
 * DTO đơn giản hóa thông tin cho vehicle assignment suggestion
 */
public record SimplifiedVehicleAssignmentResponse(
    List<VehicleSuggestionDTO> vehicleSuggestions
) {
    /**
     * DTO cho xe
     */
    public record VehicleSuggestionDTO(
        UUID id,
        String licensePlateNumber,
        String model,
        String manufacturer,
        List<DriverSuggestionDTO> suggestedDrivers
    ) {}

    /**
     * DTO cho tài xế
     */
    public record DriverSuggestionDTO(
        UUID id,
        String fullName,
        String driverLicenseNumber,
        String licenseClass
    ) {}
}
