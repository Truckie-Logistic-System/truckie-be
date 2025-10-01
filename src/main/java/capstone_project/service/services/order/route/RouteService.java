package capstone_project.service.services.order.route;

import capstone_project.dtos.request.route.SuggestRouteRequest;
import capstone_project.dtos.response.route.RoutePointsResponse;
import capstone_project.dtos.response.route.SuggestRouteResponse;

import java.util.UUID;

public interface RouteService {
    RoutePointsResponse getRoutePoints(UUID assignmentId);

    SuggestRouteResponse suggestRoute(SuggestRouteRequest request);
}
