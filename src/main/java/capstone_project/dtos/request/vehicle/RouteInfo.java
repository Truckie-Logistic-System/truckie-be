package capstone_project.dtos.request.vehicle;

import java.util.List;

/**
 * Request class representing route information with segments and total toll fee
 */
public record RouteInfo(
    List<RouteSegmentInfo> segments,
    Long totalTollFee
) {}
