package capstone_project.common.enums;

/**
 * Enum for categorizing different types of issues
 * Used to determine which fields are relevant and how to handle the issue
 */
public enum IssueCategoryEnum {
    DAMAGE,
    PENALTY,
    MISSING_ITEMS,
    WRONG_ITEMS,
    GENERAL,
    ACCIDENT,
    SEAL_REPLACEMENT,
    ORDER_REJECTION,
    REROUTE,
    OFF_ROUTE_RUNAWAY  // Driver went off-route and potentially absconded with packages
}
