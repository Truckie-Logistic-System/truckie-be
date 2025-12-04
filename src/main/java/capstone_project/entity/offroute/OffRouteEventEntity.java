package capstone_project.entity.offroute;

import capstone_project.common.enums.OffRouteWarningStatus;
import capstone_project.entity.common.BaseEntity;
import capstone_project.entity.issue.IssueEntity;
import capstone_project.entity.order.order.OrderEntity;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity to track off-route events for vehicle assignments
 * Monitors when drivers deviate from planned routes
 */
@Entity
@Table(name = "off_route_events", schema = "public", catalog = "capstone-project")
@Data
@EqualsAndHashCode(callSuper=false)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class OffRouteEventEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_assignment_id", nullable = false)
    private VehicleAssignmentEntity vehicleAssignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;

    @Column(name = "off_route_start_time", nullable = false)
    private LocalDateTime offRouteStartTime;

    @Column(name = "last_known_lat", precision = 11, scale = 8)
    private BigDecimal lastKnownLat;

    @Column(name = "last_known_lng", precision = 11, scale = 8)
    private BigDecimal lastKnownLng;

    @Column(name = "distance_from_route_meters")
    private Double distanceFromRouteMeters;

    @Column(name = "previous_distance_from_route_meters")
    private Double previousDistanceFromRouteMeters;

    @Column(name = "last_location_update_at")
    private LocalDateTime lastLocationUpdateAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "warning_status", length = 30)
    private OffRouteWarningStatus warningStatus;

    @Column(name = "yellow_warning_sent_at")
    private LocalDateTime yellowWarningSentAt;

    @Column(name = "red_warning_sent_at")
    private LocalDateTime redWarningSentAt;

    @Column(name = "can_contact_driver")
    private Boolean canContactDriver;

    @Column(name = "last_contact_attempt_at")
    private LocalDateTime lastContactAttemptAt;

    @Column(name = "contact_notes", length = 500)
    private String contactNotes;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolved_reason", length = 500)
    private String resolvedReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id")
    private IssueEntity issue;

    // Contact confirmation flow fields
    @Column(name = "contacted_at")
    private LocalDateTime contactedAt;

    @Column(name = "contacted_by")
    private UUID contactedBy;

    @Column(name = "grace_period_extended")
    @Builder.Default
    private Boolean gracePeriodExtended = false;

    @Column(name = "grace_period_extension_count", nullable = false)
    @Builder.Default
    private Integer gracePeriodExtensionCount = 0;

    @Column(name = "grace_period_extended_at")
    private LocalDateTime gracePeriodExtendedAt;

    @Column(name = "grace_period_expires_at")
    private LocalDateTime gracePeriodExpiresAt;

    /**
     * Calculate duration in minutes since off-route started
     */
    public long getOffRouteDurationMinutes() {
        if (offRouteStartTime == null) {
            return 0;
        }
        return java.time.Duration.between(offRouteStartTime, LocalDateTime.now()).toMinutes();
    }

    /**
     * Check if this event is still active (not resolved)
     */
    public boolean isActive() {
        return warningStatus != OffRouteWarningStatus.RESOLVED_SAFE 
            && warningStatus != OffRouteWarningStatus.BACK_ON_ROUTE
            && warningStatus != OffRouteWarningStatus.ISSUE_CREATED;
    }
}
