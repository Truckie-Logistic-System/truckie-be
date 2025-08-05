// src/main/java/capstone_project/controller/user/DistanceController.java
package capstone_project.controller.user;

import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.dtos.response.user.DistanceTimeResponse;
import capstone_project.dtos.response.user.RouteInstructionsResponse;
import capstone_project.service.services.user.DistanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/distance")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class DistanceController {

    private final DistanceService distanceService;

    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse<DistanceTimeResponse>> getDistanceAndTime(
            @PathVariable UUID orderId) {
        DistanceTimeResponse response = distanceService.calculateDistanceAndTime(orderId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/order/{orderId}/instructions")
    public ResponseEntity<ApiResponse<RouteInstructionsResponse>> getRouteInstructions(
            @PathVariable UUID orderId) {
        RouteInstructionsResponse response = distanceService.getRouteInstructions(orderId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/order/{orderId}/meters")
    public ResponseEntity<ApiResponse<Double>> getDistanceInMeters(
            @PathVariable UUID orderId) {
        double distance = distanceService.getDistanceInMeters(orderId);
        return ResponseEntity.ok(ApiResponse.ok(distance));
    }

    @GetMapping("/order/{orderId}/time")
    public ResponseEntity<ApiResponse<Long>> getTravelTimeInSeconds(
            @PathVariable UUID orderId) {
        long time = distanceService.getTravelTimeInSeconds(orderId);
        return ResponseEntity.ok(ApiResponse.ok(time));
    }
}