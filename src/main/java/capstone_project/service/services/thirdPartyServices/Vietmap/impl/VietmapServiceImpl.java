package capstone_project.service.services.thirdPartyServices.Vietmap.impl;

import capstone_project.service.services.thirdPartyServices.Vietmap.VietmapService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
public class VietmapServiceImpl implements VietmapService {

    private final WebClient webClient;
    private final String apiKey;
    private final String baseUrl;
    private final String autocompleteEndpoint;
    private final String placeEndpoint;
    private final String reverseEndpoint;
    private final String routeTollsEndpoint;
    private final String styleEndpoint;
    private final String DEFAULT_HCMC_FOCUS;
    private final Integer CityId_HCMC;
    private final String routeEndpoint;

    public VietmapServiceImpl(WebClient.Builder webClientBuilder,
                              @Value("${vietmap.base-url}") String baseUrl,
                              @Value("${vietmap.api.key}") String apiKey,
                              @Value("${vietmap.api.autocomplete.demo.endpoint}") String autocompleteEndpoint,
                              @Value("${vietmap.api.place.demo.endpoint}") String placeEndpoint,
                              @Value("${vietmap.api.reverse-geocode.demo.endpoint}") String reverseEndpoint,
                              @Value("${vietmap.api.route-tolls.endpoint}") String routeTollsEndpoint,
                              @Value("${vietmap.maps.styles.endpoint}") String styleEndpoint,
                              @Value("${vietmap.parameter.value.default.hcm.focus}") String defaultHcmcFocus,
                              @Value("${vietmap.parameter.value.city.id.hcm}") Integer cityId_HCMC,
                              @Value("${vietmap.api.route.endpoint}") String routeEndpoint) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.autocompleteEndpoint = autocompleteEndpoint;
        this.placeEndpoint = placeEndpoint;
        this.reverseEndpoint = reverseEndpoint;
        this.routeTollsEndpoint = routeTollsEndpoint;
        this.styleEndpoint = styleEndpoint;
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.DEFAULT_HCMC_FOCUS = defaultHcmcFocus;
        this.CityId_HCMC = cityId_HCMC;
        this.routeEndpoint = routeEndpoint;
    }

    @Override
    public String autocomplete(String text,
                               String focus,
                               Integer cityId,
                               Integer distId,
                               Integer wardId,
                               String circle_center,
                               Integer circle_radius,
                               String cats,
                               String layers) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text parameter is required");
        }

        String effectiveFocus = (focus == null || focus.isBlank()) ? DEFAULT_HCMC_FOCUS : focus;

        String uri = UriComponentsBuilder
                .fromUriString(baseUrl + autocompleteEndpoint)
                .queryParam("apikey", apiKey)
                .queryParam("text", text)
                .queryParam("cityId", CityId_HCMC)
                .queryParam("focus", effectiveFocus)
                .queryParam("display_type", 1)
                .build()
                .toUriString();

        try {
            log.info("Calling Vietmap Autocomplete API: {}", uri);
            return webClient.get().uri(uri).retrieve().bodyToMono(String.class).block();
        } catch (WebClientResponseException ex) {
            throw new RuntimeException("Vietmap API error: " + ex.getStatusCode() + " - " + ex.getResponseBodyAsString(), ex);
        }
    }

    @Override
    public String place(String refId) {
        if (refId == null || refId.isBlank()) {
            throw new IllegalArgumentException("refid parameter is required");
        }

        String uri = UriComponentsBuilder
                .fromUriString(baseUrl + placeEndpoint)
                .queryParam("apikey", apiKey)
                .queryParam("refid", refId)
                .build()
                .toUriString();

        try {
            log.info("Calling Vietmap Place API: {}", uri);
            return webClient.get().uri(uri).retrieve().bodyToMono(String.class).block();
        } catch (WebClientResponseException ex) {
            throw new RuntimeException("Vietmap Place API error: " + ex.getStatusCode() + " - " + ex.getResponseBodyAsString(), ex);
        }
    }

    @Override
    public String reverse(Double lat, Double lng) {
        if (lat == null || lng == null) {
            throw new IllegalArgumentException("lat and lng parameters are required");
        }

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(baseUrl + reverseEndpoint)
                .queryParam("apikey", apiKey)
                .queryParam("lng", lng)
                .queryParam("lat", lat)
                .queryParam("display_type", 1);

        String uri = builder.build().toUriString();

        try {
            log.info("Calling Vietmap reverse API: {}", uri);
            return webClient.get().uri(uri).retrieve().bodyToMono(String.class).block();
        } catch (WebClientResponseException ex) {
            throw new RuntimeException("Vietmap Reverse API error: " + ex.getStatusCode() + " - " + ex.getResponseBodyAsString(), ex);
        }
    }

    @Override
    public String routeTolls(String pathJson, Integer vehicle) {
        if (pathJson == null || pathJson.isBlank()) {
            throw new IllegalArgumentException("path body is required");
        }

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(baseUrl + routeTollsEndpoint)
                .queryParam("apikey", apiKey)
                .queryParam("api-version", "1.1");

        if (vehicle != null) {
            builder.queryParam("vehicle", vehicle);
        }

        String uri = builder.build().toUriString();

        try {
            log.info("Calling Vietmap Route-Tolls API: {}", uri);
            return webClient.post()
                    .uri(uri)
                    .header("Content-Type", "application/json")
                    .bodyValue(pathJson)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (WebClientResponseException ex) {
            throw new RuntimeException("Vietmap Route-Tolls API error: " + ex.getStatusCode() + " - " + ex.getResponseBodyAsString(), ex);
        }
    }

    @Override
    public String route(java.util.List<String> points,
                        Boolean pointsEncoded,
                        String vehicle,
                        Boolean optimize,
                        String avoid) {
        if (points == null || points.size() < 2) {
            throw new IllegalArgumentException("At least two point parameters are required");
        }

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(baseUrl + routeEndpoint)
                .queryParam("apikey", apiKey)
                .queryParam("api-version", "1.1");

        // add each repeated point param (expected format: "lat,lng" or "lat,lng")
        for (String p : points) {
            if (p != null && !p.isBlank()) {
                builder.queryParam("point", p);
            }
        }

        if (pointsEncoded != null) {
            builder.queryParam("points_encoded", pointsEncoded);
        }
        if (vehicle != null && !vehicle.isBlank()) {
            builder.queryParam("vehicle", vehicle);
        }
        if (optimize != null) {
            builder.queryParam("optimize", optimize);
        }
        if (avoid != null && !avoid.isBlank()) {
            builder.queryParam("avoid", avoid);
        }

        String uri = builder.build().toUriString();

        try {
            log.info("Calling Vietmap Route API: {}", uri);
            return webClient.get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (WebClientResponseException ex) {
            throw new RuntimeException("Vietmap Route API error: " + ex.getStatusCode() + " - " + ex.getResponseBodyAsString(), ex);
        }
    }

    @Override
    public String styles() {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(baseUrl + styleEndpoint)
                .queryParam("apikey", apiKey);

        String uri = builder.build().toUriString();

        try {
            log.info("Calling Vietmap style  API: {}", uri);
            return webClient.get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (WebClientResponseException ex) {
            throw new RuntimeException("Vietmap Styles API error: " + ex.getStatusCode() + " - " + ex.getResponseBodyAsString(), ex);
        }
    }
}
