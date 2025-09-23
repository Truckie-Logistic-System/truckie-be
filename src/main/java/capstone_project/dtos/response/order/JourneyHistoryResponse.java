package capstone_project.dtos.response.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record JourneyHistoryResponse(
        UUID id,
        BigDecimal startLocation,
        BigDecimal endLocation,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String status,
        BigDecimal totalDistance,
        Boolean isReportedIncident,
        Boolean isRouteChanged,
        String routeChangeReason,
        String originalRouteJson,
        String currentRouteJson,
        UUID previousJourneyId,
        UUID vehicleAssignmentId,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt
) {}
