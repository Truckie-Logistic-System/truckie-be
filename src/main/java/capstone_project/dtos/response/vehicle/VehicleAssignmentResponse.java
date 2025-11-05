package capstone_project.dtos.response.vehicle;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record VehicleAssignmentResponse(
        UUID id,
        UUID vehicleId,
        UUID driver_id_1,
        UUID driver_id_2,
        String status,
        String trackingCode,
        VehicleInfo vehicle,
        DriverInfo driver1,
        DriverInfo driver2
){
    public record VehicleInfo(
            UUID id,
            String licensePlateNumber,
            String model,
            String manufacturer,
            Integer year,
            VehicleTypeInfo vehicleType
    ) {}

    public record VehicleTypeInfo(
            UUID id,
            String vehicleTypeName
    ) {}
    
    public record DriverInfo(
            UUID id,
            String fullName,
            String phoneNumber,
            String driverLicenseNumber,
            String licenseClass,
            String experienceYears
    ) {}
}