package capstone_project.service.services.user.impl;

import capstone_project.dtos.response.user.GeocodingResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class VietMapGeocodingServiceImpl {

    private static final String LAYERS_PARAM = "CITY";
    private static final String REF_ID_KEY = "ref_id";
    private static final String LAT_KEY = "lat";
    private static final String LNG_KEY = "lng";
    private static final String CITY_KEY = "city";
    private static final String WARD_KEY = "ward";
    private static final String NAME_KEY = "name";
    private static final Duration API_TIMEOUT = Duration.ofSeconds(10);

    private final WebClient webClient;
    private final String apiKey;
    private final String searchEndpoint;
    private final String placeEndpoint;

    public VietMapGeocodingServiceImpl(WebClient.Builder webClientBuilder,
                                  @Value("${vietmap.base-url}") String baseUrl,
                                  @Value("${vietmap.api.key}") String apiKey,
                                  @Value("${vietmap.api.search.demo.endpoint}") String searchEndpoint,
                                  @Value("${vietmap.api.place.demo.endpoint}") String placeEndpoint) {
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
                .build();
        this.apiKey = apiKey;
        this.searchEndpoint = searchEndpoint;
        this.placeEndpoint = placeEndpoint;
    }

    public Optional<GeocodingResponse> geocodeAddress(String address) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("VietMap API key not configured");
            return Optional.empty();
        }

        try {
            return searchAddressAndGetRefId(address)
                    .flatMap(this::getPlaceDetailsByRefId)
                    .map(this::buildGeocodingResultFromPlaceDetails);
        } catch (WebClientResponseException e) {
            log.error("API response error for address: {} - Status: {}, Body: {}",
                    address, e.getStatusCode(), e.getResponseBodyAsString(), e);
            return Optional.empty();
        } catch (WebClientException e) {
            log.error("Network error while resolving address: {}", address, e);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Unexpected error resolving address: {}", address, e);
            return Optional.empty();
        }
    }

    private Optional<String> searchAddressAndGetRefId(String address) {
        try {
            List<Map<String, Object>> searchResponse = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(searchEndpoint)
                            .queryParam("apikey", apiKey)
                            .queryParam("text", address)
                            .queryParam("display_type", "1")
                            .queryParam("layers", LAYERS_PARAM)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                    .timeout(API_TIMEOUT)
                    .block();

            if (searchResponse != null && !searchResponse.isEmpty()) {
                Object refIdObj = searchResponse.get(0).get(REF_ID_KEY);
                if (refIdObj != null) {
                    return Optional.of(refIdObj.toString());
                }
            }

            log.warn("No ref_id found in search response for address: {}", address);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error searching for address: {}", address, e);
            return Optional.empty();
        }
    }

    private Optional<Map<String, Object>> getPlaceDetailsByRefId(String refId) {
        try {
            Map<String, Object> placeDetails = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(placeEndpoint)
                            .queryParam("apikey", apiKey)
                            .queryParam("refid", refId)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .timeout(API_TIMEOUT)
                    .block();

            if (placeDetails != null && placeDetails.containsKey(LAT_KEY) && placeDetails.containsKey(LNG_KEY)) {
                return Optional.of(placeDetails);
            }

            log.warn("Invalid place details response for refId: {}", refId);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error getting place details for refId: {}", refId, e);
            return Optional.empty();
        }
    }

    private GeocodingResponse buildGeocodingResultFromPlaceDetails(Map<String, Object> placeDetails) {
        try {
            BigDecimal latitude = new BigDecimal(placeDetails.get(LAT_KEY).toString());
            BigDecimal longitude = new BigDecimal(placeDetails.get(LNG_KEY).toString());
            String province = Optional.ofNullable(placeDetails.get(CITY_KEY))
                    .map(Object::toString)
                    .orElse("");
            String ward = Optional.ofNullable(placeDetails.get(WARD_KEY))
                    .map(Object::toString)
                    .orElse("");
            String street = Optional.ofNullable(placeDetails.get(NAME_KEY))
                    .map(Object::toString)
                    .orElse("");

            return new GeocodingResponse(latitude, longitude, province, ward, street);
        } catch (Exception e) {
            log.error("Error building geocoding result from place details", e);
            throw new RuntimeException("Failed to parse place details", e);
        }
    }
}