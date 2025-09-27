// src/main/java/capstone_project/service/services/user/DistanceService.java
package capstone_project.service.services.user;

import capstone_project.dtos.response.user.DistanceTimeResponse;
import capstone_project.dtos.response.user.RouteInstructionsResponse;

import java.math.BigDecimal;
import java.util.UUID;

public interface DistanceService {
    DistanceTimeResponse calculateDistanceAndTime(UUID orderId);
    RouteInstructionsResponse getRouteInstructions(UUID orderId);
    BigDecimal getDistanceInKilometers(UUID orderId);
    double getTravelTimeInHours(UUID orderId);
    DistanceTimeResponse calculateDistanceAndTimeByTrackAsia(UUID orderId);
    RouteInstructionsResponse getRouteInstructionsByTrackAsia(UUID orderId);
    BigDecimal getDistanceInKilometersByTrackAsia(UUID orderId);
    double getTravelTimeInHoursByTrackAsia(UUID orderId);
}