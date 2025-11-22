package capstone_project.service.services.thirdPartyServices.Vietmap.impl;

import capstone_project.dtos.request.vietmap.VietmapRouteV3Request;
import capstone_project.dtos.response.vietmap.VietmapRouteV3Response;
import capstone_project.service.services.thirdPartyServices.Vietmap.VietmapService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final String mobileStyleEndpoint;
    private final ObjectMapper objectMapper;
    private final Boolean defaultAlternative;
    private final String defaultTime;

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
                              @Value("${vietmap.api.route.endpoint}") String routeEndpoint,
                              @Value("${vietmap.maps.mobile.styles.endpoint}") String mobileStyleEndpoint,
                              @Value("${vietmap.route.default.alternative}") Boolean defaultAlternative,
                              @Value("${vietmap.route.default.time}") String defaultTime) {
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
        this.mobileStyleEndpoint = mobileStyleEndpoint;
        this.objectMapper = new ObjectMapper();
        this.defaultAlternative = defaultAlternative;
        this.defaultTime = defaultTime;
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
            
            return webClient.get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (WebClientResponseException ex) {
            throw new RuntimeException("Vietmap Styles API error: " + ex.getStatusCode() + " - " + ex.getResponseBodyAsString(), ex);
        }
    }

    @Override
    public String mobileStyles() {
        
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(baseUrl + mobileStyleEndpoint)
                .queryParam("apikey", apiKey);

        String uri = builder.build().toUriString();

        try {
            
            return webClient.get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (WebClientResponseException ex) {
            throw new RuntimeException("Vietmap Mobile Styles API error: " + ex.getStatusCode() + " - " + ex.getResponseBodyAsString(), ex);
        }
    }

    @Override
    public String getMobileStyleUrl() {
        // Return Vector style URL (best performance according to VietMap SDK docs)
        // SDK will handle caching, progressive loading, and tile optimization automatically
        String styleUrl = baseUrl + "/maps/styles/tm/style.json?apikey=" + apiKey;
        
        return styleUrl;
    }

    @Override
    public VietmapRouteV3Response routeV3(VietmapRouteV3Request request) {
        // Validate required parameters
        if (request.getPoints() == null || request.getPoints().size() < 2) {
            throw new IllegalArgumentException("At least two point parameters are required");
        }

        // Build URI with query parameters
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(baseUrl + routeEndpoint)
                .queryParam("apikey", apiKey);

        // Add each point (format: "lat,lng")
        for (String point : request.getPoints()) {
            if (point != null && !point.isBlank()) {
                builder.queryParam("point", point);
            }
        }

        // Add optional parameters with defaults
        if (request.getPointsEncoded() != null) {
            builder.queryParam("points_encoded", request.getPointsEncoded());
        } else {
            // Default to true for better performance
            builder.queryParam("points_encoded", true);
        }
        
        if (request.getVehicle() != null && !request.getVehicle().isBlank()) {
            builder.queryParam("vehicle", request.getVehicle());
        }
        
        if (request.getOptimize() != null) {
            builder.queryParam("optimize", request.getOptimize());
        }
        
        if (request.getCapacity() != null) {
            builder.queryParam("capacity", request.getCapacity());
        }
        
        if (request.getTime() != null && !request.getTime().isBlank()) {
            builder.queryParam("time", request.getTime());
        } else {
            builder.queryParam("time", defaultTime);
        }
        
        if (request.getAlternative() != null) {
            builder.queryParam("alternative", request.getAlternative());
        } else {
            // Default to true to get alternative routes
            builder.queryParam("alternative", defaultAlternative);
        }
        
        if (request.getHeading() != null) {
            builder.queryParam("heading", request.getHeading());
        }
        
        if (request.getAnnotations() != null && !request.getAnnotations().isBlank()) {
            builder.queryParam("annotations", request.getAnnotations());
        }
        
        if (request.getAvoid() != null && !request.getAvoid().isBlank()) {
            builder.queryParam("avoid", request.getAvoid());
        }

        String uri = builder.build().toUriString();

        try {
            log.info("Calling Vietmap Route API v3: {}", uri);
            
            String jsonResponse = webClient.get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            // Parse JSON response to VietmapRouteV3Response
            VietmapRouteV3Response response = objectMapper.readValue(jsonResponse, VietmapRouteV3Response.class);
            
            log.info("Vietmap Route API v3 response code: {}", response.getCode());
            return response;
            
        } catch (WebClientResponseException ex) {
            log.error("Vietmap Route API v3 error: {} - {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new RuntimeException("Vietmap Route API v3 error: " + ex.getStatusCode() + " - " + ex.getResponseBodyAsString(), ex);
        } catch (Exception ex) {
            log.error("Error parsing Vietmap Route API v3 response", ex);
            throw new RuntimeException("Error parsing Vietmap Route API v3 response: " + ex.getMessage(), ex);
        }
    }
}
