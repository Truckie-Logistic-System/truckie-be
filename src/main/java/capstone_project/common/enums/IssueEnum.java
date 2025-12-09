package capstone_project.common.enums;

public enum IssueEnum {
    OPEN, 
    IN_PROGRESS, 
    RESOLVED, 
    PAYMENT_OVERDUE,
    RESOLVED_SAFE,      // Off-route issue resolved - driver confirmed safe
    RUNAWAY_CONFIRMED,  // Driver absconded with packages - compensation required
    CLOSED_FRAUD,       // Issue closed due to confirmed fraud
    EXPIRED             // Issue expired beyond complaint deadline
}
