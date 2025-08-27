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
    public double getDistanceInMeters(UUID orderId) {
        return calculateDistanceAndTime(orderId).distance();
    }

    @Override
    public long getTravelTimeInSeconds(UUID orderId) {
        return calculateDistanceAndTime(orderId).time();
    }

    private RouteResponse callVietMapApi(UUID orderId) {
        OrderEntity order = orderEntityService.findContractRuleEntitiesById(orderId)
                .orElseThrow(() -> new NotFoundException(
                ErrorEnum.NOT_FOUND.getMessage(),
                ErrorEnum.NOT_FOUND.getErrorCode()
        ));

        AddressEntity pickup = order.getPickupAddress();
        AddressEntity delivery = order.getDeliveryAddress();

        if (pickup == null || delivery == null) {
            throw new IllegalStateException("Order is missing pickup or delivery address");
        }

        String uri = buildRouteUri(
                pickup.getLatitude().doubleValue(),
                pickup.getLongitude().doubleValue(),
                delivery.getLatitude().doubleValue(),
                delivery.getLongitude().doubleValue()
        );

        return webClient.get()
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
    }

    private String buildRouteUri(double startLat, double startLng, double endLat, double endLng) {
        return UriComponentsBuilder.fromHttpUrl(baseUrl + routeEndpoint)
                .queryParam("api-version", "1.1")
                .queryParam("apikey", apiKey)
                .queryParam("point", String.format("%f,%f", startLat, startLng))
                .queryParam("point", String.format("%f,%f", endLat, endLng))
                .queryParam("vehicle", "car")
                .build()
                .toUriString();
    }
}