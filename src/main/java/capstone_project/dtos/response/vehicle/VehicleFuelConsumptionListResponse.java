package capstone_project.dtos.response.vehicle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for listing vehicle fuel consumptions for staff
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleFuelConsumptionListResponse {
    private UUID id;
    private BigDecimal fuelVolumeLiters;
    private String odometerAtStartUrl;
    private String odometerAtEndUrl;
    private String companyInvoiceImageUrl;
    private BigDecimal odometerStartKm;
    private BigDecimal odometerEndKm;
    private BigDecimal distanceTraveledKm;
    private LocalDateTime dateRecorded;
    private String notes;
    private LocalDateTime createdAt;
    
    // Vehicle Assignment info
    private VehicleAssignmentInfo vehicleAssignment;
    
    // Vehicle info
    private VehicleInfo vehicle;
    
    // Driver info
    private DriverInfo driver;
    
    // Fuel Type info
    private FuelTypeInfo fuelType;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VehicleAssignmentInfo {
        private UUID id;
        private String trackingCode;
        private String status;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VehicleInfo {
        private UUID id;
        private String licensePlateNumber;
        private String vehicleType;
        private String brand;
        private String model;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DriverInfo {
        private UUID id;
        private String fullName;
        private String phoneNumber;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FuelTypeInfo {
        private UUID id;
        private String name;
        private String description;
    }
}
