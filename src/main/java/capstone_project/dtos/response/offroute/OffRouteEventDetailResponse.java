package capstone_project.dtos.response.offroute;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Full detail response for off-route event modal
 * Contains all information needed for staff to assess the situation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OffRouteEventDetailResponse {
    private UUID id;
    private String warningStatus;
    private long offRouteDurationMinutes;
    private LocalDateTime offRouteStartTime;
    private Boolean canContactDriver;
    private String contactNotes;
    
    // Location
    private LocationInfo currentLocation;
    private List<RouteSegmentInfo> plannedRouteSegments;
    
    // Trip Info
    private TripInfo tripInfo;
    
    // Driver Info
    private DriverInfo driverInfo;
    
    // Vehicle Info
    private VehicleInfo vehicleInfo;
    
    // Order Info
    private OrderInfo orderInfo;
    
    // Package Info
    private List<PackageInfo> packages;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocationInfo {
        private BigDecimal lat;
        private BigDecimal lng;
        private Double distanceFromRouteMeters;
        private LocalDateTime lastUpdatedAt;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RouteSegmentInfo {
        private Integer segmentOrder;
        private String startPointName;
        private String endPointName;
        private BigDecimal startLat;
        private BigDecimal startLng;
        private BigDecimal endLat;
        private BigDecimal endLng;
        private String pathCoordinatesJson;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TripInfo {
        private UUID vehicleAssignmentId;
        private String trackingCode;
        private String status;
        private LocalDateTime startTime;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DriverInfo {
        private UUID driverId;
        private String fullName;
        private String phoneNumber;
        private String licenseNumber;
        private String avatarUrl;
    }
    
    // Support for both drivers
    private DriverInfo driver1Info;
    private DriverInfo driver2Info;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VehicleInfo {
        private UUID vehicleId;
        private String licensePlate;
        private String vehicleType;
        private String vehicleTypeDescription;
        private String manufacturer;
        private BigDecimal loadCapacityKg;
        private String model;
        private Integer yearOfManufacture;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderInfo {
        private UUID orderId;
        private String orderCode;
        private String status;
        private BigDecimal totalContractAmount;
        private BigDecimal totalDeclaredValueOfTrip;
        
        // Sender (Customer/Company)
        private String senderName;
        private String senderPhone;
        private String senderCompanyName;
        private String senderAddress;
        private String senderProvince;
        
        // Receiver
        private String receiverName;
        private String receiverPhone;
        private String receiverIdentity; // CCCD
        private String receiverAddress;
        private String receiverProvince;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PackageInfo {
        private UUID orderDetailId;
        private String trackingCode;
        private String description;
        private BigDecimal weight;
        private String weightUnit;
        private String status;
        private BigDecimal declaredValue;
    }
}
