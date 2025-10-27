package capstone_project.controller.user;

import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.dtos.response.user.DistanceTimeResponse;
import capstone_project.dtos.response.user.RouteInstructionsResponse;
import capstone_project.service.services.user.DistanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("${distance.api.base-path}")
@RequiredArgsConstructor
//@PreAuthorize("hasRole('ADMIN')")
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

    @GetMapping("/order/{orderId}/kilometers")
    public ResponseEntity<ApiResponse<BigDecimal>> getDistanceInKilometers(
            @PathVariable UUID orderId) {
        BigDecimal distance = distanceService.getDistanceInKilometers(orderId);
        return ResponseEntity.ok(ApiResponse.ok(distance));
    }

    @GetMapping("/order/{orderId}/time")
    public ResponseEntity<ApiResponse<Double>> getTravelTimeInHours(
            @PathVariable UUID orderId) {
        double time = distanceService.getTravelTimeInHours(orderId);
        return ResponseEntity.ok(ApiResponse.ok(time));
    }
}