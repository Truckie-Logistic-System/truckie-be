package capstone_project.repository.repositories.offroute;

import capstone_project.common.enums.OffRouteWarningStatus;
import capstone_project.entity.offroute.OffRouteEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OffRouteEventRepository extends JpaRepository<OffRouteEventEntity, UUID> {

    /**
     * Find active off-route event for a vehicle assignment
     */
    @Query("SELECT e FROM OffRouteEventEntity e WHERE e.vehicleAssignment.id = :vehicleAssignmentId " +
           "AND e.warningStatus NOT IN ('RESOLVED_SAFE', 'BACK_ON_ROUTE', 'ISSUE_CREATED', 'CONTACT_FAILED') " +
           "ORDER BY e.offRouteStartTime DESC")
    Optional<OffRouteEventEntity> findActiveByVehicleAssignmentId(@Param("vehicleAssignmentId") UUID vehicleAssignmentId);

    /**
     * Find all active off-route events (for scheduled monitoring)
     */
    @Query("SELECT e FROM OffRouteEventEntity e WHERE e.warningStatus NOT IN ('RESOLVED_SAFE', 'BACK_ON_ROUTE', 'ISSUE_CREATED', 'CONTACT_FAILED')")
    List<OffRouteEventEntity> findAllActiveEvents();

    /**
     * Find events by warning status
     */
    List<OffRouteEventEntity> findByWarningStatus(OffRouteWarningStatus status);

    /**
     * Find events by order ID
     */
    List<OffRouteEventEntity> findByOrderId(UUID orderId);

    /**
     * Find events by vehicle assignment ID (all history)
     */
    List<OffRouteEventEntity> findByVehicleAssignmentId(UUID vehicleAssignmentId);

    /**
     * Find event by ID with full details
     */
    @Query("SELECT e FROM OffRouteEventEntity e " +
           "LEFT JOIN FETCH e.vehicleAssignment va " +
           "LEFT JOIN FETCH va.driver1 d1 " +
           "LEFT JOIN FETCH d1.user u1 " +
           "LEFT JOIN FETCH va.vehicleEntity v " +
           "LEFT JOIN FETCH v.vehicleTypeEntity vt " +
           "LEFT JOIN FETCH e.order o " +
           "LEFT JOIN FETCH o.sender s " +
           "LEFT JOIN FETCH o.pickupAddress pa " +
           "LEFT JOIN FETCH o.deliveryAddress da " +
           "WHERE e.id = :eventId")
    Optional<OffRouteEventEntity> findByIdWithFullDetails(@Param("eventId") UUID eventId);

    /**
     * Count active events for an order
     */
    @Query("SELECT COUNT(e) FROM OffRouteEventEntity e WHERE e.order.id = :orderId " +
           "AND e.warningStatus NOT IN ('RESOLVED_SAFE', 'BACK_ON_ROUTE', 'ISSUE_CREATED', 'CONTACT_FAILED')")
    long countActiveByOrderId(@Param("orderId") UUID orderId);

    /**
     * Find off-route event by issue ID
     */
    @Query("SELECT e FROM OffRouteEventEntity e WHERE e.issue.id = :issueId")
    Optional<OffRouteEventEntity> findByIssueId(@Param("issueId") UUID issueId);

    /**
     * Find events with specific warning status and expired grace period
     */
    @Query("SELECT e FROM OffRouteEventEntity e WHERE e.warningStatus = :status " +
           "AND e.gracePeriodExpiresAt < :currentTime")
    List<OffRouteEventEntity> findByWarningStatusAndGracePeriodExpiresBefore(
        @Param("status") OffRouteWarningStatus status, 
        @Param("currentTime") java.time.LocalDateTime currentTime
    );
}
