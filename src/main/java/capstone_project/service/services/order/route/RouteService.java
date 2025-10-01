package capstone_project.service.services.order.route;

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

    SuggestRouteResponse suggestRoute(SuggestRouteRequest request);
}
