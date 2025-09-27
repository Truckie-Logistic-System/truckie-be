package capstone_project.service.services.user.impl;

import capstone_project.common.enums.ErrorEnum;
import capstone_project.dtos.response.user.DistanceTimeResponse;
import capstone_project.dtos.response.user.RouteInstructionsResponse;
import capstone_project.dtos.response.user.RouteResponse;
import capstone_project.entity.order.order.OrderEntity;
import capstone_project.entity.user.address.AddressEntity;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.repository.entityServices.order.order.OrderEntityService;
import capstone_project.service.services.thirdPartyServices.TrackAsia.TrackAsiaService;
import capstone_project.service.services.user.DistanceService;
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
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class DistanceServiceImpl implements DistanceService {

    private final WebClient webClient;
    private final OrderEntityService orderEntityService;
    private final TrackAsiaService trackAsiaService;

    @Value("${vietmap.api.base-url}")
    private String baseUrl;

    @Value("${vietmap.api.key}")
    private String apiKey;

    @Value("${vietmap.api.route.endpoint}")
    private String routeEndpoint;

    @Value("${trackasia.api.directions.v2.endpoint}")
    private String trackAsiaBaseUrl;

    @Value("${trackasia.api.key}")
    private String trackAsiapiKey;

    @Override
    public DistanceTimeResponse calculateDistanceAndTimeByTrackAsia(UUID orderId) {
        RouteResponse response = callTrackAsiaApi(orderId);
        return DistanceTimeResponse.fromRouteResponse(response);
    }

    @Override
    public RouteInstructionsResponse getRouteInstructionsByTrackAsia(UUID orderId) {
        RouteResponse response = callTrackAsiaApi(orderId);
        return RouteInstructionsResponse.fromRouteResponse(response);
    }

    @Override
    public BigDecimal getDistanceInKilometersByTrackAsia(UUID orderId) {
        return calculateDistanceAndTime(orderId).distance();
    }

    @Override
    public double getTravelTimeInHoursByTrackAsia(UUID orderId) {
        return calculateDistanceAndTime(orderId).time();
    }

    private RouteResponse callTrackAsiaApi(UUID orderId) {
        log.info("Fetching order with ID: {}", orderId);
        OrderEntity order = orderEntityService.findEntityById(orderId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        AddressEntity pickup = order.getPickupAddress();
        AddressEntity delivery = order.getDeliveryAddress();

        log.info("Pickup address: {}", pickup);
        log.info("Delivery address: {}", delivery);

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

        String origin = String.format(Locale.US, "%f,%f", pickup.getLatitude().doubleValue(), pickup.getLongitude().doubleValue());
        String destination = String.format(Locale.US, "%f,%f", delivery.getLatitude().doubleValue(), delivery.getLongitude().doubleValue());

        log.info("Calling TrackAsia directions: origin='{}' destination='{}' endpoint='{}'", origin, destination, trackAsiaBaseUrl);

        try {
            JsonNode body = trackAsiaService.directions(origin, destination, "car", "json", true);
            log.info("TrackAsia directions response: {}", body);

            if (body == null || body.isMissingNode()) {
                log.error("Empty TrackAsia response for order {}", orderId);
                return new RouteResponse(
                        List.of(new RouteResponse.Path(
                                0.0, 0, 0.0,
                                false,
                                List.of(),
                                List.of()
                        )),
                        "ERROR",
                        "Empty TrackAsia response"
                );
            }

            // Try to map TrackAsia response directly to RouteResponse if shapes match
            ObjectMapper mapper = new ObjectMapper();
            try {
                RouteResponse mapped = mapper.treeToValue(body, RouteResponse.class);
                if (mapped != null) {
                    return mapped;
                }
            } catch (Exception e) {
                log.debug("Direct mapping from TrackAsia response to RouteResponse failed: {}", e.getMessage());
            }

            // Fallback: if mapping fails, return an error-like RouteResponse so callers can handle it.
            return new RouteResponse(
                    List.of(new RouteResponse.Path(
                            0.0, 0, 0.0,
                            false,
                            List.of(),
                            List.of()
                    )),
                    "ERROR",
                    "Failed to convert TrackAsia response to internal RouteResponse"
            );
        } catch (Exception e) {
            log.error("Unexpected error calling TrackAsia directions: {}", e.getMessage(), e);
            throw e;
        }
    }

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
        log.info("Fetching order with ID: {}", orderId);
        OrderEntity order = orderEntityService.findEntityById(orderId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        AddressEntity pickup = order.getPickupAddress();
        AddressEntity delivery = order.getDeliveryAddress();

        log.info("Pickup address: {}", pickup);
        log.info("Delivery address: {}", delivery);

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

        String uri = buildRouteUri(
                pickup.getLatitude().doubleValue(),
                pickup.getLongitude().doubleValue(),
                delivery.getLatitude().doubleValue(),
                delivery.getLongitude().doubleValue()
        );

        log.info("Calling VietMap API with URI: {}", uri);

        try {
            RouteResponse response = webClient.get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(RouteResponse.class)
                    .onErrorResume(e -> {
                        log.error("Error calling VietMap API: {}", e.getMessage(), e);
                        return Mono.just(new RouteResponse(
                                List.of(new RouteResponse.Path(
                                        0.0, 0, 0.0,
                                        false,
                                        List.of(),
                                        List.of()
                                )),
                                "ERROR",
                                "Failed to calculate distance: " + e.getMessage()
                        ));
                    })
                    .block();

            log.info("VietMap API response: {}", response);
            return response;
        } catch (Exception e) {
            log.error("Unexpected error calling VietMap API: {}", e.getMessage(), e);
            throw e;
        }
    }

    private String buildRouteUri(double startLat, double startLng, double endLat, double endLng) {
        // Format coordinates with US locale to ensure dot as decimal separator
        String startPoint = String.format(Locale.US, "%f,%f", startLat, startLng);
        String endPoint = String.format(Locale.US, "%f,%f", endLat, endLng);

        log.debug("Building route URI with points: {} to {}", startPoint, endPoint);

        String uri = UriComponentsBuilder.fromHttpUrl(baseUrl + routeEndpoint)
                .queryParam("api-version", "1.1")
                .queryParam("apikey", apiKey)
                .queryParam("point", startPoint)
                .queryParam("point", endPoint)
                .queryParam("vehicle", "car")
                .build()
                .toUriString();

        log.debug("Built URI: {}", uri);
        return uri;
    }
}