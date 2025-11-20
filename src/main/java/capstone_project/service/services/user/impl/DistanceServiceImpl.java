package capstone_project.service.services.user.impl;

import capstone_project.common.enums.ErrorEnum;
import capstone_project.dtos.response.user.DistanceTimeResponse;
import capstone_project.dtos.response.user.RouteInstructionsResponse;
import capstone_project.dtos.response.user.RouteResponse;
import capstone_project.entity.order.order.OrderEntity;
import capstone_project.entity.user.address.AddressEntity;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.repository.entityServices.order.order.OrderEntityService;
import capstone_project.service.services.user.DistanceService;
import capstone_project.service.services.thirdPartyServices.Vietmap.VietmapService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class DistanceServiceImpl implements DistanceService {

    private final WebClient webClient;
    private final OrderEntityService orderEntityService;
    private final VietmapService vietmapService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${vietmap.base-url}")
    private String baseUrl;

    @Value("${vietmap.api.key}")
    private String apiKey;

    @Value("${vietmap.api.route.endpoint}")
    private String routeEndpoint;
    
    private static final int EARTH_RADIUS_KM = 6371;

    @Override
    public DistanceTimeResponse calculateDistanceAndTime(UUID orderId) {
        RouteResponse response = callVietMapApi(orderId);
        return DistanceTimeResponse.fromRouteResponse(response);
    }

    @Override
    public RouteInstructionsResponse getRouteInstructions(UUID orderId) {
        RouteResponse response = callVietMapApi(orderId);
        return RouteInstructionsResponse.fromRouteResponse(response);
    }

    @Override
    public BigDecimal getDistanceInKilometers(UUID orderId) {
        return calculateDistanceAndTime(orderId).distance();
    }

    @Override
    public double getTravelTimeInHours(UUID orderId) {
        return calculateDistanceAndTime(orderId).time();
    }

    private RouteResponse callVietMapApi(UUID orderId) {
        
        OrderEntity order = orderEntityService.findEntityById(orderId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        AddressEntity pickup = order.getPickupAddress();
        AddressEntity delivery = order.getDeliveryAddress();

        if (pickup == null || delivery == null) {
            log.error("Order {} is missing pickup or delivery address", orderId);
            throw new IllegalStateException("Order is missing pickup or delivery address");
        }

        if (pickup.getLatitude() == null || pickup.getLongitude() == null ||
                delivery.getLatitude() == null || delivery.getLongitude() == null) {
            log.error("Order {} has null coordinates. Pickup: lat={}, lng={}, Delivery: lat={}, lng={}",
                    orderId,
                    pickup.getLatitude(), pickup.getLongitude(),
                    delivery.getLatitude(), delivery.getLongitude());
            throw new IllegalStateException("Order has invalid coordinates");
        }

        try {
            // Use route-tolls API for consistency with route planning
            double pickupLng = pickup.getLongitude().doubleValue();
            double pickupLat = pickup.getLatitude().doubleValue();
            double deliveryLng = delivery.getLongitude().doubleValue();
            double deliveryLat = delivery.getLatitude().doubleValue();
            
            // Build path JSON for route-tolls API: [[lng, lat], [lng, lat]]
            List<List<Double>> path = Arrays.asList(
                Arrays.asList(pickupLng, pickupLat),
                Arrays.asList(deliveryLng, deliveryLat)
            );
            
            String pathJson = objectMapper.writeValueAsString(path);

            // Call route-tolls API (vehicle type null = car by default)
            String vietmapResponse = vietmapService.routeTolls(pathJson, null);

            // Parse response and convert to RouteResponse format
            return parseRouteTollsResponse(vietmapResponse);
            
        } catch (Exception e) {
            log.error("Error calling VietMap route-tolls API: {}", e.getMessage(), e);
            // Fallback to Haversine calculation
            return createFallbackResponse(
                pickup.getLatitude().doubleValue(),
                pickup.getLongitude().doubleValue(),
                delivery.getLatitude().doubleValue(),
                delivery.getLongitude().doubleValue()
            );
        }
    }

    /**
     * Parse route-tolls API response and convert to RouteResponse format
     */
    private RouteResponse parseRouteTollsResponse(String jsonResponse) throws Exception {
        JsonNode root = objectMapper.readTree(jsonResponse);
        
        // Extract path coordinates from response
        JsonNode pathNode = root.path("path");
        List<List<Double>> pathCoordinates = new ArrayList<>();
        
        if (pathNode.isArray()) {
            for (JsonNode coord : pathNode) {
                if (coord.isArray() && coord.size() >= 2) {
                    double lng = coord.get(0).asDouble();
                    double lat = coord.get(1).asDouble();
                    pathCoordinates.add(Arrays.asList(lng, lat));
                }
            }
        }
        
        // Calculate distance from path coordinates
        double distanceKm = calculatePathDistance(pathCoordinates);
        double distanceMeters = distanceKm * 1000;

        // Create RouteResponse.Path with calculated distance
        RouteResponse.Path path = new RouteResponse.Path(
            distanceMeters,  // distance in meters
            0,               // time (not available from route-tolls)
            0.0,             // weight
            false,           // points_encoded
            List.of(),       // bbox
            List.of()        // instructions
        );
        
        return new RouteResponse(
            List.of(path),
            "OK",
            "Distance calculated from route-tolls API"
        );
    }
    
    /**
     * Calculate total distance along a path using Haversine formula
     */
    private double calculatePathDistance(List<List<Double>> path) {
        if (path == null || path.size() < 2) {
            return 0.0;
        }
        
        double totalDistance = 0.0;
        for (int i = 0; i < path.size() - 1; i++) {
            List<Double> point1 = path.get(i);
            List<Double> point2 = path.get(i + 1);
            
            if (point1.size() >= 2 && point2.size() >= 2) {
                double distance = calculateHaversineDistance(
                    point1.get(1), point1.get(0),  // lat, lng
                    point2.get(1), point2.get(0)   // lat, lng
                );
                totalDistance += distance;
            }
        }
        
        return totalDistance;
    }
    
    /**
     * Calculate distance between two points using Haversine formula
     */
    private double calculateHaversineDistance(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }
    
    /**
     * Create fallback response using Haversine distance calculation
     */
    private RouteResponse createFallbackResponse(double pickupLat, double pickupLng, 
                                                  double deliveryLat, double deliveryLng) {
        log.warn("Creating fallback response using Haversine calculation");
        
        double distanceKm = calculateHaversineDistance(pickupLat, pickupLng, deliveryLat, deliveryLng);
        double distanceMeters = distanceKm * 1000;
        
        RouteResponse.Path path = new RouteResponse.Path(
            distanceMeters,
            0,
            0.0,
            false,
            List.of(),
            List.of()
        );
        
        return new RouteResponse(
            List.of(path),
            "FALLBACK",
            "Distance calculated using Haversine formula (route-tolls API failed)"
        );
    }
}