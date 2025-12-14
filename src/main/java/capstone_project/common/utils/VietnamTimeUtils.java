package capstone_project.common.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for handling Vietnam timezone (UTC+7) consistently across the application.
 * This ensures all time-related operations use the same timezone regardless of server location.
 */
public final class VietnamTimeUtils {
    
    public static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    
    private VietnamTimeUtils() {
        // Utility class - prevent instantiation
    }
    
    /**
     * Get current LocalDateTime in Vietnam timezone
     */
    public static LocalDateTime now() {
        return LocalDateTime.now(VIETNAM_ZONE);
    }
    
    /**
     * Get current LocalDate in Vietnam timezone
     */
    public static LocalDate today() {
        return LocalDate.now(VIETNAM_ZONE);
    }
    
    /**
     * Format LocalDateTime to string with pattern
     */
    public static String format(LocalDateTime dateTime, String pattern) {
        if (dateTime == null) return "";
        return dateTime.format(DateTimeFormatter.ofPattern(pattern));
    }
    
    /**
     * Format current time with pattern (commonly used for generating codes)
     */
    public static String formatNow(String pattern) {
        return now().format(DateTimeFormatter.ofPattern(pattern));
    }
}
