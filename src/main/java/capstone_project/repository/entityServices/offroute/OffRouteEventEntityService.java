package capstone_project.repository.entityServices.offroute;

import capstone_project.common.enums.OffRouteWarningStatus;
import capstone_project.entity.offroute.OffRouteEventEntity;
import capstone_project.repository.entityServices.common.BaseEntityService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OffRouteEventEntityService extends BaseEntityService<OffRouteEventEntity, UUID> {

    /**
     * Find by ID (convenience method that delegates to findEntityById)
     */
    default Optional<OffRouteEventEntity> findById(UUID id) {
        return findEntityById(id);
    }

    /**
     * Find active off-route event for a vehicle assignment
     */
    Optional<OffRouteEventEntity> findActiveByVehicleAssignmentId(UUID vehicleAssignmentId);

    /**
     * Find all active off-route events
     */
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
     * Find events by vehicle assignment ID
     */
    List<OffRouteEventEntity> findByVehicleAssignmentId(UUID vehicleAssignmentId);

    /**
     * Find event by ID with full details
     */
    Optional<OffRouteEventEntity> findByIdWithFullDetails(UUID eventId);

    /**
     * Count active events for an order
     */
    long countActiveByOrderId(UUID orderId);

    /**
     * Find off-route event by issue ID
     */
    Optional<OffRouteEventEntity> findByIssueId(UUID issueId);

    /**
     * Find events with specific warning status and expired grace period
     */
    List<OffRouteEventEntity> findByWarningStatusAndGracePeriodExpiresBefore(
        OffRouteWarningStatus status, 
        java.time.LocalDateTime currentTime
    );
}
