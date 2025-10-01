package capstone_project.controller.route;

import capstone_project.dtos.request.route.SuggestRouteRequest;
import capstone_project.dtos.response.route.RoutePointsResponse;
import capstone_project.dtos.response.route.SuggestRouteResponse;
import capstone_project.service.services.order.route.RouteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("${route.api.base-path}")
public class RouteController {

    private final RouteService routeService;

    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    @GetMapping("/assignments/{assignmentId}/points")
    public ResponseEntity<RoutePointsResponse> getRoutePoints(@PathVariable UUID assignmentId) {
        RoutePointsResponse response = routeService.getRoutePoints(assignmentId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/suggest")
    public ResponseEntity<SuggestRouteResponse> suggestRoute(@Valid @RequestBody SuggestRouteRequest request) {
        SuggestRouteResponse response = routeService.suggestRoute(request);
        return ResponseEntity.ok(response);
    }
}
