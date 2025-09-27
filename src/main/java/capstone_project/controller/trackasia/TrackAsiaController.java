package capstone_project.controller.trackasia;

import capstone_project.dtos.response.trackasia.AutoComplete.TrackAsiaAutocompleteResponse;
import capstone_project.dtos.response.trackasia.PlaceDetails.TrackAsiaPlaceDetailsResponse;
import capstone_project.dtos.response.trackasia.ReverseGeocoding.TrackAsiaReverseGeocodeResponse;
import capstone_project.dtos.response.trackasia.Search.TrackAsiaSearchResponse;
import capstone_project.service.services.thirdPartyServices.TrackAsia.TrackAsiaService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${trackasia.api.base-path}")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class TrackAsiaController {

    private final TrackAsiaService trackAsiaService;

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
