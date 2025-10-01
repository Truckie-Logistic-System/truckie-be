package capstone_project.dtos.request.vehicle;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request class representing information about a route segment
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
    List<List<BigDecimal>> pathCoordinates,
    Long estimatedTollFee
) {}
