package capstone_project.common.enums;

public enum VehicleAssignmentEnum {
    UNASSIGNED,        // Vehicle not assigned to any driver/route
    ASSIGNED_TO_DRIVER, // Assigned directly to a driver
    ASSIGNED_TO_ROUTE,  // Assigned to a route or trip
    RESERVED,           // Reserved for upcoming assignment
    IN_TRANSIT,         // Currently on an active trip
    ON_STANDBY,         // Idle but ready for assignment
    MAINTENANCE_HOLD,   // Blocked due to maintenance
    DECOMMISSIONED,
    ASSIGNED,// Permanently retired, cannot be reassigned
    COMPLETE
}
