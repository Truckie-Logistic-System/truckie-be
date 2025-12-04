package capstone_project.common.enums;

/**
 * Status enum for off-route warning events
 * Tracks the progression of off-route detection and resolution
 */
public enum OffRouteWarningStatus {
    NONE,               // Initial state, no warning sent
    YELLOW_SENT,        // 5-minute warning sent to staff
    RED_SENT,           // 10-minute warning sent to staff
    CONTACTED_WAITING_RETURN, // Staff has contacted driver, waiting for return to route
    CONTACT_FAILED,     // Driver failed to return within grace period, escalate to issue
    RESOLVED_SAFE,      // Staff confirmed driver is safe after contact
    ISSUE_CREATED,      // Issue created from off-route event
    BACK_ON_ROUTE       // Driver returned to planned route automatically
}
