package capstone_project.dtos.response.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for driver overview in chat (for staff view)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverOverviewResponse {
    
    // User info
    private UUID userId;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String imageUrl;
    private Boolean gender;
    private LocalDate dateOfBirth;
    private LocalDateTime memberSince;
    
    // Driver info
    private UUID driverId;
    private String identityNumber;
    private String driverLicenseNumber;
    private String licenseClass;
    private LocalDateTime dateOfExpiry;
    private String driverStatus;
    
    // Delivery statistics
    private Integer totalOrdersReceived;
    private Integer totalTripsCompleted;
    private Integer successfulDeliveries;
    private Double successRate;
    private Integer cancelledDeliveries;
    private Double cancelRate;
    private Integer issuesCount;
    private Integer penaltiesCount;
    
    // Recent trips/orders
    private List<RecentTripInfo> recentTrips;
    
    // Active trips (PICKING_UP and beyond)
    private List<ActiveTripInfo> activeTrips;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentTripInfo {
        private UUID vehicleAssignmentId;
        private String trackingCode;
        private String orderCode;
        private String status;
        private String vehicleType;
        private String pickupAddress;
        private String deliveryAddress;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private Boolean isActive;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActiveTripInfo {
        private UUID vehicleAssignmentId;
        private String trackingCode;
        private String orderCode;
        private String status;
        private String receiverName;
        private String receiverPhone;
        private String currentLocation;
        private LocalDateTime expectedDelivery;
    }
}
