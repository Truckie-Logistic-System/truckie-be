package capstone_project.service.services.route;

import capstone_project.dtos.request.route.SuggestRouteRequest;
import capstone_project.dtos.response.route.RoutePointsResponse;
import capstone_project.dtos.response.route.SuggestRouteResponse;

import java.util.UUID;

public interface RouteService {
    /**
     * Get route points by vehicle assignment ID
     * @deprecated Use getRoutePointsByOrder instead
     */
    RoutePointsResponse getRoutePoints(UUID assignmentId);

    /**
     * Get route points by order ID
     * This method retrieves carrier, pickup, and delivery points based on order information
     * @param orderId The ID of the order
     * @return Response containing the route points (carrier, pickup, delivery)
     */
    RoutePointsResponse getRoutePointsByOrder(UUID orderId);

    /**
     * Get route points by issue ID for return route
     * This method retrieves carrier, delivery (as pickup), and original pickup (as delivery) points for return journey
     * @param issueId The ID of the issue (ORDER_REJECTION)
     * @return Response containing the return route points
     */
    RoutePointsResponse getRoutePointsByIssue(UUID issueId);

    SuggestRouteResponse suggestRoute(SuggestRouteRequest request);
}
