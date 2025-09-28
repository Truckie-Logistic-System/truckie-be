package capstone_project.controller.trackasia;

import capstone_project.dtos.response.trackasia.AutoComplete.TrackAsiaAutocompleteResponse;
import capstone_project.dtos.response.trackasia.PlaceDetails.TrackAsiaPlaceDetailsResponse;
import capstone_project.dtos.response.trackasia.ReverseGeocoding.TrackAsiaReverseGeocodeResponse;
import capstone_project.dtos.response.trackasia.Search.TrackAsiaSearchResponse;
import capstone_project.service.services.thirdPartyServices.TrackAsia.TrackAsiaService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Iterator;
import java.util.Map;

@RestController
@RequestMapping("${trackasia.api.base-path}")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class TrackAsiaController {

    private final TrackAsiaService trackAsiaService;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${trackasia.api.key}")
    private String apiKey;

    @Value("${trackasia.base.url}")
    private String trackasiaBaseUrl;

    @Value("${app.base.url}")
    private String appBaseUrl;

    @GetMapping("/style")
    public ResponseEntity<JsonNode> getMapStyle(@RequestParam(defaultValue = "streets") String styleType) {
        try {
            String url = String.format("%s/styles/v2/%s.json?key=%s", trimTrailingSlash(trackasiaBaseUrl), styleType, apiKey);
            ResponseEntity<JsonNode> response = restTemplate.getForEntity(url, JsonNode.class);
            JsonNode styleJson = response.getBody();
            if (styleJson == null) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Empty style response from TrackAsia");
            }

            String proxyBaseUrl = trimTrailingSlash(appBaseUrl) + "/api/trackasia";

            if (styleJson.has("sprite") && styleJson instanceof ObjectNode) {
                ((ObjectNode) styleJson).put("sprite", proxyBaseUrl + "/sprites/v2/" + styleType);
            }

            if (styleJson.has("glyphs") && styleJson instanceof ObjectNode) {
                ((ObjectNode) styleJson).put("glyphs", proxyBaseUrl + "/fonts/{fontstack}/{range}");
            }

            JsonNode sources = styleJson.get("sources");
            if (sources != null && sources.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> fields = sources.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> entry = fields.next();
                    JsonNode source = entry.getValue();
                    if (source.has("tiles") && source.get("tiles").isArray()) {
                        ArrayNode tilesArray = (ArrayNode) source.get("tiles");
                        for (int i = 0; i < tilesArray.size(); i++) {
                            String tile = tilesArray.get(i).asText();
                            String newTile = tile.replace(trimTrailingSlash(trackasiaBaseUrl), proxyBaseUrl);
                            tilesArray.set(i, objectMapper.valueToTree(newTile));
                        }
                    }
                }
            }

            return ResponseEntity.ok(styleJson);
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(objectMapper.createObjectNode());
        }
    }

    private static String trimTrailingSlash(String s) {
        if (s == null) return "";
        return s.replaceAll("/+$", "");
    }

    @GetMapping("/directions")
    public ResponseEntity<JsonNode> directions(
            @RequestParam("origin") String origin,
            @RequestParam("destination") String destination,
            @RequestParam(value = "mode", required = false) String mode) {

        if (origin == null || origin.trim().isEmpty() || destination == null || destination.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        JsonNode response = trackAsiaService.directions(
                origin.trim(),
                destination.trim(),
                (mode == null || mode.trim().isEmpty()) ? "driving" : mode.trim(),
                "json",
                null // pass null so service applies default new_admin=true
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<TrackAsiaSearchResponse> search(@RequestParam("query") String query) {
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        TrackAsiaSearchResponse response = trackAsiaService.search(query.trim());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/autocomplete")
    public ResponseEntity<TrackAsiaAutocompleteResponse> autocomplete(
            @RequestParam("input") String input,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestParam(value = "bounds", required = false) String bounds,
            @RequestParam(value = "location", required = false) String location) {

        if (input == null || input.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        String cleanedInput = input.trim();
        Integer finalSize = (size == null || size <= 0) ? 5 : size;
        String cleanedBounds = (bounds == null || bounds.trim().isEmpty()) ? null : bounds.trim();
        String cleanedLocation = (location == null || location.trim().isEmpty()) ? null : location.trim();

        // Pass null for newAdmin and includeOldAdmin so defaults in service are applied
        TrackAsiaAutocompleteResponse response = trackAsiaService.autocomplete(
                cleanedInput,
                finalSize,
                cleanedBounds,
                cleanedLocation,
                true,
                null
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/geocode/reverse")
    public ResponseEntity<TrackAsiaReverseGeocodeResponse> reverseGeocode(
            @RequestParam("latlng") String latlng,
            @RequestParam(value = "radius", required = false) Integer radius) {

        if (latlng == null || latlng.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        TrackAsiaReverseGeocodeResponse response = trackAsiaService.reverseGeocode(
                latlng.trim(),
                radius,
                true,
                null
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/place/details")
    public ResponseEntity<TrackAsiaPlaceDetailsResponse> placeDetails(
            @RequestParam("place_id") String placeId) {

        if (placeId == null || placeId.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        TrackAsiaPlaceDetailsResponse response = trackAsiaService.placeDetails(
                placeId.trim(),
                "json",
                true,
                null
        );

        return ResponseEntity.ok(response);
    }
}
