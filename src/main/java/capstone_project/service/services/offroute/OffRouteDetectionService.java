package capstone_project.service.services.offroute;

import capstone_project.dtos.response.offroute.OffRouteEventDetailResponse;
import capstone_project.dtos.response.offroute.OffRouteWarningPayload;
import capstone_project.entity.offroute.OffRouteEventEntity;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Service interface for off-route detection and management
 */
public interface OffRouteDetectionService {

    /**
     * Process a location update for off-route detection
     * @param vehicleAssignmentId Vehicle assignment ID
     * @param lat Current latitude
     * @param lng Current longitude
     * @param speed Current speed in km/h (optional)
     * @param bearing Current bearing in degrees (optional)
     * @return Calculated distance from route in meters, or null if no route available
     */
    Double processLocationUpdate(UUID vehicleAssignmentId, BigDecimal lat, BigDecimal lng, 
                               Double speed, Double bearing);

    /**
     * Get full details of an off-route event for staff modal
     */
    OffRouteEventDetailResponse getEventDetail(UUID eventId);

    /**
     * Mark off-route event as resolved (driver confirmed safe)
     */
    OffRouteEventEntity confirmSafe(UUID eventId, String notes, UUID staffId);

    /**
     * Mark that driver could not be contacted
     */
    OffRouteEventEntity markNoContact(UUID eventId, String notes, UUID staffId);

    /**
     * Create an issue from an off-route event
     */
    UUID createIssueFromEvent(UUID eventId, String description, UUID staffId);

    /**
     * Check all active events and send warnings if thresholds are crossed
     * Called by scheduled job
     */
    void checkAndSendWarnings();

    /**
     * Reset off-route event when driver returns to route
     */
    void resetOffRouteEvent(UUID vehicleAssignmentId);

    /**
     * Confirm staff has contacted driver for off-route event
     */
    OffRouteEventEntity confirmContact(UUID eventId, UUID staffId);

    /**
     * Extend grace period for driver to return to route
     */
    OffRouteEventEntity extendGracePeriod(UUID eventId, UUID staffId);

    /**
     * Check contacted waiting return events and handle grace period expiration
     * Called by scheduled job
     */
    void checkContactedWaitingReturnEvents();
}
