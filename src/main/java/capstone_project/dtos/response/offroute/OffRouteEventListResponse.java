package capstone_project.dtos.response.offroute;

import capstone_project.common.enums.OffRouteWarningStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for listing off-route events for staff
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OffRouteEventListResponse {
    private UUID id;
    private LocalDateTime offRouteStartTime;
    private BigDecimal lastKnownLat;
    private BigDecimal lastKnownLng;
    private Double distanceFromRouteMeters;
    private OffRouteWarningStatus warningStatus;
    private LocalDateTime yellowWarningSentAt;
    private LocalDateTime redWarningSentAt;
    private Boolean canContactDriver;
    private LocalDateTime contactedAt;
    private LocalDateTime resolvedAt;
    private String resolvedReason;
    private LocalDateTime gracePeriodExpiresAt;
    private Integer gracePeriodExtensionCount;
    private LocalDateTime createdAt;
    
    // Vehicle Assignment info
    private VehicleAssignmentInfo vehicleAssignment;
    
    // Order info
    private OrderInfo order;
    
    // Issue info (if created)
    private IssueInfo issue;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VehicleAssignmentInfo {
        private UUID id;
        private String trackingCode;
        private String status;
        private String vehiclePlateNumber;
        private String driverName;
        private String driverPhone;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderInfo {
        private UUID id;
        private String orderCode;
        private String status;
        private String senderName;
        private String receiverName;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IssueInfo {
        private UUID id;
        private String issueTypeName;
        private String status;
    }
}
