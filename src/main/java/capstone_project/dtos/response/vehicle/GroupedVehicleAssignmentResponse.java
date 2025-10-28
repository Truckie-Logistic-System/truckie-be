package capstone_project.dtos.response.vehicle;

import java.util.List;
import java.util.UUID;

/**
 * DTO for grouped vehicle assignment suggestions that shows which order details
 * should be assigned to the same vehicle assignment
 */
public record GroupedVehicleAssignmentResponse(
    List<OrderDetailGroup> groups
) {
    /**
     * Represents a group of order details that should be assigned to the same vehicle assignment
     */
    public record OrderDetailGroup(
        List<OrderDetailInfo> orderDetails,
        List<VehicleSuggestionResponse> suggestedVehicles,
        String groupingReason
    ) {}

    /**
     * Basic information about an order detail
     */
    public record OrderDetailInfo(
        UUID id,
        String trackingCode,
        Double weightBaseUnit,
        String unit,
        String description
    ) {}

    /**
     * DTO for vehicle suggestions with isRecommended flag
     */
    public record VehicleSuggestionResponse(
        UUID id,
        String licensePlateNumber,
        String model,
        String manufacturer,
        UUID vehicleTypeId,
        String vehicleTypeName,
        List<DriverSuggestionResponse> suggestedDrivers,
        boolean isRecommended
    ) {}

    /**
     * DTO for driver suggestions with isRecommended flag and additional info
     */
    public record DriverSuggestionResponse(
        UUID id,
        String fullName,
        String driverLicenseNumber,
        String licenseClass,
        boolean isRecommended,
        int violationCount,
        int completedTripsCount,
        String experienceYears,
        String lastActiveTime
    ) {}
}
