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
 * WebSocket payload for off-route warning sent to staff
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OffRouteWarningPayload {
    private String type; // OFF_ROUTE_WARNING
    private String severity; // YELLOW or RED
    private UUID offRouteEventId;
    private UUID vehicleAssignmentId;
    private UUID orderId;
    private long offRouteDurationSeconds;
    private LocationInfo lastKnownLocation;
    
    // Driver info
    private String driverName;
    private String driverPhone;
    private String driverLicenseNumber;
    
    // Vehicle info
    private String vehiclePlate;
    private String vehicleType;
    private String vehicleManufacturer;
    
    // Order info
    private String orderCode;
    private int packageCount;
    private BigDecimal totalContractAmount;
    private BigDecimal totalDeclaredValue;
    
    // Sender/Receiver brief
    private String senderName;
    private String senderPhone;
    private String receiverName;
    private String receiverPhone;
    
    private LocalDateTime warningTime;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocationInfo {
        private BigDecimal lat;
        private BigDecimal lng;
        private Double distanceFromRouteMeters;
    }
}
