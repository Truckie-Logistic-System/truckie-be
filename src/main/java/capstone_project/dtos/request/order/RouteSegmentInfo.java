package capstone_project.dtos.request.order;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for route segment information in journey creation
 */
public record RouteSegmentInfo(
        Integer segmentOrder,
        String startPointName,
        String endPointName,
        BigDecimal startLatitude,
        BigDecimal startLongitude,
        BigDecimal endLatitude,
        BigDecimal endLongitude,
        Integer distanceMeters,
        String pathCoordinatesJson,
        List<Object> tollDetails, // Can be empty list or actual toll details
        BigDecimal estimatedTollFee // Estimated toll fee for this segment
) {}
