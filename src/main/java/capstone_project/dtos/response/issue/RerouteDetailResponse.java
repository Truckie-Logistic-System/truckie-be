package capstone_project.dtos.response.issue;

import capstone_project.dtos.response.order.JourneyHistoryResponse;
import capstone_project.dtos.response.order.JourneySegmentResponse;
import capstone_project.dtos.response.vehicle.VehicleAssignmentResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for REROUTE issue detail
 * Contains: affected segment, active journey with full route, vehicle/driver info
 */
public record RerouteDetailResponse(
        UUID issueId,
        String status,
        String description,
        LocalDateTime reportedAt,
        LocalDateTime resolvedAt,
        BigDecimal locationLatitude,
        BigDecimal locationLongitude,
        
        // Affected segment full info (to highlight on map)
        JourneySegmentResponse affectedSegment,
        
        // Vehicle assignment with vehicle & driver details
        VehicleAssignmentResponse vehicleAssignment,
        
        // Latest ACTIVE journey with full segments (to draw full route on map)
        JourneyHistoryResponse activeJourney,
        
        // New journey info (after staff processes reroute - null if not yet resolved)
        JourneyHistoryResponse reroutedJourney,
        
        // Issue images (optional for REROUTE)
        List<String> issueImages
) {
}
