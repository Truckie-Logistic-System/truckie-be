package capstone_project.dtos.response.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for vehicle assignment quick view in chat (for staff view)
 * Contains comprehensive trip information across multiple tabs
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleAssignmentQuickViewResponse {
    
    // ==================== Basic Info ====================
    private UUID vehicleAssignmentId;
    private String trackingCode;
    private String status;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    
    // ==================== Tab 1: Vehicle & Driver ====================
    private VehicleInfo vehicleInfo;
    private DriverInfo primaryDriver;
    private DriverInfo secondaryDriver;
    
    // ==================== Tab 2: Orders & Packages ====================
    private List<OrderInfo> orders;
    
    // ==================== Tab 3: Issues ====================
    private List<IssueInfo> issues;
    
    // ==================== Tab 4: Proofs & Seals ====================
    private List<ProofInfo> packingProofs;
    private List<ProofInfo> photoCompletions;
    private List<SealInfo> seals;
    
    // ==================== Tab 5: Journey & Route ====================
    private List<JourneyHistoryInfo> journeyHistory;
    private List<JourneySegmentInfo> journeySegments;
    private FuelConsumptionInfo fuelConsumption;
    
    // ==================== Nested Classes ====================
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VehicleInfo {
        private UUID vehicleId;
        private String licensePlateNumber;
        private String model;
        private String manufacturer;
        private Integer year;
        private BigDecimal capacity;
        private String status;
        private String vehicleTypeName;
        private String fuelTypeName;
        private BigDecimal currentLatitude;
        private BigDecimal currentLongitude;
        private LocalDateTime lastUpdated;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DriverInfo {
        private UUID driverId;
        private UUID userId;
        private String fullName;
        private String phoneNumber;
        private String email;
        private String imageUrl;
        private String identityNumber;
        private String driverLicenseNumber;
        private String licenseClass;
        private String status;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderInfo {
        private UUID orderId;
        private String orderCode;
        private String status;
        private String categoryName;
        private String categoryDescription;
        
        // Sender info
        private String senderName;
        private String senderPhone;
        private String pickupAddress;
        private BigDecimal pickupLatitude;
        private BigDecimal pickupLongitude;
        
        // Receiver info
        private String receiverName;
        private String receiverPhone;
        private String deliveryAddress;
        private BigDecimal deliveryLatitude;
        private BigDecimal deliveryLongitude;
        
        // Order details (packages)
        private List<PackageInfo> packages;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PackageInfo {
        private UUID orderDetailId;
        private String trackingCode;
        private String status;
        private String description;
        private BigDecimal weightTons;
        private String weightUnit;
        private BigDecimal declaredValue;
        private String sizeName;
        private String sizeDescription;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IssueInfo {
        private UUID issueId;
        private String issueTypeName;
        private String issueCategory;
        private String description;
        private String status;
        private LocalDateTime reportedAt;
        private LocalDateTime resolvedAt;
        private BigDecimal locationLatitude;
        private BigDecimal locationLongitude;
        private List<String> issueImages;
        private String resolutionNote;
        
        // Seal replacement info
        private String oldSealCode;
        private String newSealCode;
        private String sealRemovalImage;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProofInfo {
        private UUID id;
        private String type; // PACKING_PROOF, PHOTO_COMPLETION
        private String imageUrl;
        private String description;
        private LocalDateTime capturedAt;
        private String capturedBy;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SealInfo {
        private UUID sealId;
        private String sealCode;
        private String status;
        private String sealType;
        private LocalDateTime assignedAt;
        private LocalDateTime verifiedAt;
        private String verifiedBy;
        private String imageUrl;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JourneyHistoryInfo {
        private UUID journeyId;
        private String status;
        private LocalDateTime timestamp;
        private BigDecimal latitude;
        private BigDecimal longitude;
        private String locationName;
        private String notes;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JourneySegmentInfo {
        private UUID segmentId;
        private Integer sequenceOrder;
        private String startLocationName;
        private BigDecimal startLatitude;
        private BigDecimal startLongitude;
        private String endLocationName;
        private BigDecimal endLatitude;
        private BigDecimal endLongitude;
        private BigDecimal distanceKm;
        private Integer estimatedDurationMinutes;
        private String status;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FuelConsumptionInfo {
        private BigDecimal totalDistanceKm;
        private BigDecimal estimatedFuelLiters;
        private BigDecimal actualFuelLiters;
        private BigDecimal fuelEfficiency; // km per liter
        private BigDecimal fuelCost;
    }
}
