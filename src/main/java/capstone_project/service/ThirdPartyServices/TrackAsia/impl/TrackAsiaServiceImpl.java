package capstone_project.service.ThirdPartyServices.TrackAsia.impl;

import capstone_project.dtos.response.trackasia.AutoComplete.TrackAsiaAutocompleteResponse;
import capstone_project.dtos.response.trackasia.PlaceDetails.TrackAsiaPlaceDetailsResponse;
import capstone_project.dtos.response.trackasia.ReverseGeocoding.TrackAsiaReverseGeocodeResponse;
import capstone_project.dtos.response.trackasia.Search.TrackAsiaSearchResponse;
import capstone_project.service.ThirdPartyServices.TrackAsia.TrackAsiaService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class TrackAsiaServiceImpl implements TrackAsiaService {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String searchEndpoint;
    private final String autocompleteEndpoint;
    private final String reverseGeocodeEndpoint;
    private final String placeDetailsEndpoint;
    private final String directionsEndpoint;
    private final String apiKey;

    public TrackAsiaServiceImpl(RestTemplateBuilder builder,
                                @Value("${trackasia.api.base-url}") String baseUrl,
                                @Value("${trackasia.api.search.endpoint}") String searchEndpoint,
                                @Value("${trackasia.api.autocomplete.endpoint}") String autocompleteEndpoint,
                                @Value("${trackasia.api.reverse-geocode.endpoint}") String reverseGeocodeEndpoint,
                                @Value("${trackasia.api.place.details.endpoint}") String placeDetailsEndpoint,
                                @Value("${trackasia.api.directions.v2.endpoint}") String directionsEndpoint,
                                @Value("${trackasia.api.key}") String apiKey) {
        this.restTemplate = builder.build();
        this.baseUrl = baseUrl != null ? baseUrl : "";
        this.searchEndpoint = searchEndpoint != null ? searchEndpoint : "/place/textsearch";
        this.autocompleteEndpoint = autocompleteEndpoint != null ? autocompleteEndpoint : "/place/autocomplete";
        this.reverseGeocodeEndpoint = reverseGeocodeEndpoint != null ? reverseGeocodeEndpoint : "/reversegeocode";
        this.placeDetailsEndpoint = placeDetailsEndpoint != null ? placeDetailsEndpoint : "/place/details";
        this.directionsEndpoint = directionsEndpoint != null ? directionsEndpoint : "/route/v2/directions";
        this.apiKey = apiKey != null ? apiKey : "";
    }

    @Override
    public JsonNode directions(String origin, String destination, String mode, String outputFormat, Boolean newAdmin) {
        try {
            String fmt = (outputFormat == null || outputFormat.trim().isEmpty()) ? "json" : outputFormat.trim();
            String encodedKey = URLEncoder.encode(apiKey, StandardCharsets.UTF_8);

            java.util.function.Function<String, String> encodeLatLngOrAddress = (val) -> {
                if (val == null || val.trim().isEmpty()) return "";
                String cleaned = val.trim();
                if (cleaned.contains(",")) {
                    String[] parts = cleaned.split("\\s*,\\s*");
                    String lat = parts.length > 0 ? URLEncoder.encode(parts[0], StandardCharsets.UTF_8) : "";
                    String lng = parts.length > 1 ? URLEncoder.encode(parts[1], StandardCharsets.UTF_8) : "";
                    return lat + "," + lng;
                } else {
                    // address or single token - encode fully but restore any encoded comma just in case
                    return URLEncoder.encode(cleaned, StandardCharsets.UTF_8).replace("%2C", ",");
                }
            };

            String encodedOrigin = encodeLatLngOrAddress.apply(origin);
            String encodedDestination = encodeLatLngOrAddress.apply(destination);

            StringBuilder qs = new StringBuilder();
            if (!encodedOrigin.isEmpty()) qs.append("origin=").append(encodedOrigin);
            if (!encodedDestination.isEmpty()) {
                if (qs.length() > 0) qs.append("&");
                qs.append("destination=").append(encodedDestination);
            }
            // default new_admin true if null
            boolean finalNewAdmin = (newAdmin == null) ? true : newAdmin;
            if (qs.length() > 0) qs.append("&");
            qs.append("new_admin=").append(finalNewAdmin);

            if (mode != null && !mode.trim().isEmpty()) {
                qs.append("&mode=").append(URLEncoder.encode(mode.trim(), StandardCharsets.UTF_8));
            }

            qs.append("&key=").append(encodedKey);

            String fullUrl = String.format("%s/%s?%s",
                    directionsEndpoint,
                    fmt,
                    qs.toString());

            log.info("TrackAsia directions call: origin='{}' destination='{}' url='{}'", origin, destination, fullUrl);

            ResponseEntity<JsonNode> resp = restTemplate.getForEntity(fullUrl, JsonNode.class);
            JsonNode body = resp.getBody();
            return body != null ? body : new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode();
        } catch (Exception ex) {
            log.error("Error calling TrackAsia directions", ex);
            return new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode();
        }
    }

    @Override
    public TrackAsiaSearchResponse search(String query) {
        try {
            String encodedKey = URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
            String encodedQuery = URLEncoder.encode(query == null ? "" : query, StandardCharsets.UTF_8);
            log.info("Searching TrackAsiaMap for query: {}", query);
            log.info("baseUrl: {}", baseUrl);
            log.info("searchEndpoint: {}", searchEndpoint);
            log.info("Full URL: {}{}?language=en&key={}&query={}&new_admin=true",
                    trimSlash(baseUrl),
                    trimLeadingSlash(searchEndpoint),
                    encodedKey,
                    encodedQuery);
            String url = String.format("%s%s/json?language=en&key=%s&query=%s&new_admin=true",
                    trimSlash(baseUrl),
                    trimLeadingSlash(searchEndpoint),
                    encodedKey,
                    encodedQuery);

            ResponseEntity<TrackAsiaSearchResponse> resp = restTemplate.getForEntity(url, TrackAsiaSearchResponse.class);
            TrackAsiaSearchResponse body = resp.getBody();
            return body != null ? body : new TrackAsiaSearchResponse();
        } catch (Exception ex) {
            // swallow and return empty response so callers can handle absence
            log.error("Error calling TrackAsia search", ex);
            return new TrackAsiaSearchResponse();
        }
    }

    @Override
    public TrackAsiaAutocompleteResponse autocomplete(String input,
                                                      Integer size,
                                                      String bounds,
                                                      String location,
                                                      Boolean newAdmin,
                                                      Boolean includeOldAdmin) {
        try {
            String encodedKey = URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
            String encodedInput = URLEncoder.encode(input == null ? "" : input, StandardCharsets.UTF_8);

            StringBuilder qs = new StringBuilder();
            qs.append("input=").append(encodedInput);
            qs.append("&key=").append(encodedKey);
            if (size != null) qs.append("&size=").append(size);
            if (bounds != null && !bounds.isEmpty()) qs.append("&bounds=").append(URLEncoder.encode(bounds, StandardCharsets.UTF_8));
            if (location != null && !location.isEmpty()) qs.append("&location=").append(URLEncoder.encode(location, StandardCharsets.UTF_8));

            boolean finalNewAdmin = (newAdmin == null) ? true : newAdmin;
            boolean finalIncludeOldAdmin = (includeOldAdmin == null) ? false : includeOldAdmin;
            qs.append("&new_admin=").append(finalNewAdmin);
            qs.append("&include_old_admin=").append(finalIncludeOldAdmin);

            qs.append("&language=en");

            String fullUrl = String.format("%s%s/json?%s",
                    trimSlash(baseUrl),
                    trimLeadingSlash(autocompleteEndpoint),
                    qs);

            log.info("TrackAsia autocomplete call: input='{}' url='{}'", input, fullUrl);

            ResponseEntity<TrackAsiaAutocompleteResponse> resp = restTemplate.getForEntity(fullUrl, TrackAsiaAutocompleteResponse.class);
            TrackAsiaAutocompleteResponse body = resp.getBody();
            return body != null ? body : new TrackAsiaAutocompleteResponse();
        } catch (Exception ex) {
            log.error("Error calling TrackAsia autocomplete", ex);
            return new TrackAsiaAutocompleteResponse();
        }
    }

    @Override
    public TrackAsiaReverseGeocodeResponse reverseGeocode(String latlng,
                                                          Integer radius,
                                                          Boolean newAdmin,
                                                          Boolean includeOldAdmin) {
        try {
            String encodedKey = URLEncoder.encode(apiKey, StandardCharsets.UTF_8);

            String encodedLatlng;
            if (latlng == null || latlng.trim().isEmpty()) {
                encodedLatlng = "";
            } else {
                String cleaned = latlng.trim();
                if (cleaned.contains(",")) {
                    String[] parts = cleaned.split("\\s*,\\s*");
                    String lat = parts.length > 0 ? URLEncoder.encode(parts[0], StandardCharsets.UTF_8) : "";
                    String lng = parts.length > 1 ? URLEncoder.encode(parts[1], StandardCharsets.UTF_8) : "";
                    encodedLatlng = lat + "," + lng;
                } else {
                    // fallback: encode but restore comma if encoded
                    encodedLatlng = URLEncoder.encode(cleaned, StandardCharsets.UTF_8).replace("%2C", ",");
                }
            }

            StringBuilder qs = new StringBuilder();
            qs.append("latlng=").append(encodedLatlng);
            qs.append("&key=").append(encodedKey);
            if (radius != null && radius > 0) qs.append("&radius=").append(radius);

            boolean finalNewAdmin = (newAdmin == null) ? true : newAdmin;
            boolean finalIncludeOldAdmin = (includeOldAdmin == null) ? false : includeOldAdmin;
            qs.append("&new_admin=").append(finalNewAdmin);
            qs.append("&include_old_admin=").append(finalIncludeOldAdmin);
            qs.append("&language=en");

            String fullUrl = String.format("%s%s/json?%s",
                    trimSlash(baseUrl),
                    trimLeadingSlash(reverseGeocodeEndpoint),
                    qs.toString());

            log.info("TrackAsia reverse geocode call: latlng='{}' url='{}'", latlng, fullUrl);

            ResponseEntity<TrackAsiaReverseGeocodeResponse> resp = restTemplate.getForEntity(fullUrl, TrackAsiaReverseGeocodeResponse.class);
            TrackAsiaReverseGeocodeResponse body = resp.getBody();
            return body != null ? body : new TrackAsiaReverseGeocodeResponse();
        } catch (Exception ex) {
            log.error("Error calling TrackAsia reverse geocode", ex);
            return new TrackAsiaReverseGeocodeResponse();
        }
    }

    @Override
    public TrackAsiaPlaceDetailsResponse placeDetails(String placeId, String outputFormat, Boolean newAdmin, Boolean includeOldAdmin) {
        try {
            String fmt = (outputFormat == null || outputFormat.trim().isEmpty()) ? "json" : outputFormat.trim();
            String encodedKey = URLEncoder.encode(apiKey, StandardCharsets.UTF_8);

            String encodedPlaceId;
            if (placeId == null || placeId.trim().isEmpty()) {
                encodedPlaceId = "";
            } else {
                encodedPlaceId = URLEncoder.encode(placeId.trim(), StandardCharsets.UTF_8).replace("%3A", ":");
            }

            boolean finalNewAdmin = (newAdmin == null) ? true : newAdmin;
            boolean finalIncludeOldAdmin = (includeOldAdmin == null) ? false : includeOldAdmin;

            StringBuilder qs = new StringBuilder();
            qs.append("place_id=").append(encodedPlaceId);
            qs.append("&key=").append(encodedKey);
            qs.append("&new_admin=").append(finalNewAdmin);
            qs.append("&include_old_admin=").append(finalIncludeOldAdmin);
            qs.append("&language=en");

            String fullUrl = String.format("%s%s/%s?%s",
                    trimSlash(baseUrl),
                    trimLeadingSlash(placeDetailsEndpoint),
                    fmt,
                    qs);

            log.info("TrackAsia place details call: placeId='{}' url='{}'", placeId, fullUrl);

            ResponseEntity<TrackAsiaPlaceDetailsResponse> resp = restTemplate.getForEntity(fullUrl, TrackAsiaPlaceDetailsResponse.class);
            TrackAsiaPlaceDetailsResponse body = resp.getBody();
            return body != null ? body : new TrackAsiaPlaceDetailsResponse();
        } catch (Exception ex) {
            log.error("Error calling TrackAsia place details", ex);
            return new TrackAsiaPlaceDetailsResponse();
        }
    }

    private String trimSlash(String s) {
        if (s == null) return "";
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    private String trimLeadingSlash(String s) {
        if (s == null) return "";
        return s.startsWith("/") ? s : "/" + s;
    }
}
