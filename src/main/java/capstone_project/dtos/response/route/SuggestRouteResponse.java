package capstone_project.dtos.response.route;

import java.util.List;

public record SuggestRouteResponse(
        List<RouteSegmentResponse> segments,
        Long totalToll
) {}
