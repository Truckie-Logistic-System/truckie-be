package capstone_project.dtos.response.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO cho segment của hành trình (JourneySegment)
 */
public record JourneySegmentResponse(
    UUID id,
    Integer segmentOrder,
    String startPointName,
    String endPointName,
    BigDecimal startLatitude,
    BigDecimal startLongitude,
    BigDecimal endLatitude,
    BigDecimal endLongitude,
    Integer distanceMeters,
    String pathCoordinatesJson,
    String tollDetailsJson,
    String status,
    LocalDateTime createdAt,
    LocalDateTime modifiedAt
) {}
