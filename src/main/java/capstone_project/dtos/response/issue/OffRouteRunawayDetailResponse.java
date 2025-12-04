package capstone_project.dtos.response.issue;

import capstone_project.dtos.response.user.CustomerResponse;
import capstone_project.dtos.response.vehicle.VehicleAssignmentResponse;
import capstone_project.dtos.response.refund.GetRefundResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for OFF_ROUTE_RUNAWAY issue detail
 * Contains all packages in the trip for potential refund processing
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OffRouteRunawayDetailResponse {
    
    private UUID issueId;
    private String description;
    private String status;
    private LocalDateTime reportedAt;
    private LocalDateTime resolvedAt;
    
    // Location where off-route was detected
    private BigDecimal locationLatitude;
    private BigDecimal locationLongitude;
    
    // Off-route event details
    private OffRouteEventInfo offRouteEventInfo;
    
    // Vehicle assignment info
    private VehicleAssignmentResponse vehicleAssignment;
    
    // Sender/Customer information for refund
    private CustomerResponse sender;
    
    // All packages in the trip
    private List<PackageInfo> packages;
    private BigDecimal totalDeclaredValue;
    
    // Refund info (if processed)
    private GetRefundResponse refund;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OffRouteEventInfo {
        private UUID eventId;
        private LocalDateTime detectedAt;
        private Long offRouteDurationMinutes;
        private BigDecimal distanceFromRouteMeters;
        private String warningStatus;
        private Boolean canContactDriver;
        private String contactNotes;
        private LocalDateTime contactedAt;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PackageInfo {
        private UUID orderDetailId;
        private String trackingCode;
        private String description;
        private BigDecimal weightBaseUnit;
        private String unit;
        private BigDecimal declaredValue;
        private String status;
    }
}
