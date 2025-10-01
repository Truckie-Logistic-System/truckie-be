package capstone_project.dtos.response.route;

import java.util.List;
import java.util.UUID;

public record RoutePointsResponse(
        UUID vehicleAssignmentId,
        String trackingCode,
        List<RoutePointResponse> points
) {}
