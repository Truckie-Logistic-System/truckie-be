package capstone_project.service.services.user.impl;

import capstone_project.common.enums.ErrorEnum;
import capstone_project.dtos.response.user.DistanceTimeResponse;
import capstone_project.dtos.response.user.RouteInstructionsResponse;
import capstone_project.dtos.response.user.RouteResponse;
import capstone_project.entity.order.order.OrderEntity;
import capstone_project.entity.user.address.AddressEntity;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.service.entityServices.order.order.OrderEntityService;
import capstone_project.service.services.user.DistanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class DistanceServiceImpl implements DistanceService {

    private final WebClient webClient;
    private final OrderEntityService orderEntityService;

    @Value("${vietmap.api.base-url}")
    private String baseUrl;

    @Value("${vietmap.api.key}")
    private String apiKey;

    @Value("${vietmap.api.route.endpoint}")
    private String routeEndpoint;

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
    public double getDistanceInKilometers(UUID orderId) {
        return calculateDistanceAndTime(orderId).distance();
    }

    @Override
    public double getTravelTimeInHours(UUID orderId) {
        return calculateDistanceAndTime(orderId).time();
    }

    private RouteResponse callVietMapApi(UUID orderId) {
        log.info("Fetching order with ID: {}", orderId);
        OrderEntity order = orderEntityService.findContractRuleEntitiesById(orderId)
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