// src/main/java/capstone_project/service/services/user/DistanceService.java
package capstone_project.service.services.user;

import capstone_project.dtos.response.user.DistanceTimeResponse;
import capstone_project.dtos.response.user.RouteInstructionsResponse;
import java.util.UUID;

public interface DistanceService {
    DistanceTimeResponse calculateDistanceAndTime(UUID orderId);
    RouteInstructionsResponse getRouteInstructions(UUID orderId);
    double getDistanceInKilometers(UUID orderId);
    double getTravelTimeInHours(UUID orderId);
}