package capstone_project.dtos.response.issue;

import capstone_project.dtos.response.order.JourneyHistoryResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for REROUTE issue detail
 * Contains information about the affected segment and new journey
 */
public record RerouteDetailResponse(
        UUID issueId,
        String issueCode,
        String description,
        String status,
        LocalDateTime reportedAt,
        LocalDateTime resolvedAt,
        
        // Affected segment info
        UUID affectedSegmentId,
        String affectedSegmentStartName,
        String affectedSegmentEndName,
        BigDecimal reportedLatitude,
        BigDecimal reportedLongitude,
        
        // New journey info (after staff processes)
        JourneyHistoryResponse newJourney
) {
}
