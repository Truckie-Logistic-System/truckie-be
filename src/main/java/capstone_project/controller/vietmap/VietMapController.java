package capstone_project.controller.vietmap;

import capstone_project.dtos.request.vietmap.VietmapRouteV3Request;
import capstone_project.dtos.response.vietmap.VietmapRouteV3Response;
import capstone_project.service.services.thirdPartyServices.Vietmap.VietmapService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${vietmap.api.base-path}")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class VietMapController {

    private final VietmapService vietmapService;
    private final ObjectMapper objectMapper;

    @GetMapping(value = "/autocomplete", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> autocomplete(
            @RequestParam String text
    ) {
        if (text == null || text.isBlank()) {
            return ResponseEntity.badRequest().body("{\"error\":\"text parameter is required\"}");
        }
        try {
            String response = vietmapService.autocomplete(
                    text,
                    null,
                    null, // cityId
                    null, // distId
                    null, // wardId
                    null, // circle_center
                    null, // circle_radius
                    null, // cats
                    null  // layers
            );
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("{\"error\":\"failed to call Vietmap API\",\"detail\":\"" + ex.getMessage() + "\"}");
        }
    }

    @GetMapping(value = "/place", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> place(
            @RequestParam(name = "refid") String refid
    ) {
        if (refid == null || refid.isBlank()) {
            return ResponseEntity.badRequest().body("{\"error\":\"refid parameter is required\"}");
        }
        try {
            String response = vietmapService.place(refid);
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("{\"error\":\"failed to call Vietmap Place API\",\"detail\":\"" + ex.getMessage() + "\"}");
        }
    }

    @GetMapping(value = "/reverse", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> reverse(
            @RequestParam Double lat,
            @RequestParam Double lng
    ) {
        if (lat == null || lng == null) {
            return ResponseEntity.badRequest().body("{\"error\":\"lat and lng parameters are required\"}");
        }
        try {
            String response = vietmapService.reverse(lat, lng);
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("{\"error\":\"failed to call Vietmap Reverse API\",\"detail\":\"" + ex.getMessage() + "\"}");
        }
    }

    @PostMapping(value = "/route-tolls", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> routeTolls(
            @RequestBody List<List<Double>> path,
            @RequestParam(required = false) Integer vehicle
    ) {
        if (path == null || path.isEmpty()) {
            return ResponseEntity.badRequest().body("{\"error\":\"path body is required and must contain coordinate pairs\"}");
        }
        try {
            String pathJson = objectMapper.writeValueAsString(path);
            String response = vietmapService.routeTolls(pathJson, vehicle);
            return ResponseEntity.ok(response);
        } catch (JsonProcessingException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"error\":\"invalid path body\"}");
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("{\"error\":\"failed to call Vietmap Route-Tolls API\",\"detail\":\"" + ex.getMessage() + "\"}");
        }
    }

    @GetMapping(value = "/route", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> route(
            @RequestParam(name = "point") java.util.List<String> points,
            @RequestParam(name = "vehicle", required = false) String vehicle
    ) {
        if (points == null || points.size() < 2) {
            return ResponseEntity.badRequest()
                    .body("{\"error\":\"at least two point query parameters are required (e.g. &point=lat,lng&point=lat,lng)\"}");
        }
        try {
            String response = vietmapService.route(points, false, vehicle, false, null);
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("{\"error\":\"failed to call Vietmap Route API\",\"detail\":\"" + ex.getMessage() + "\"}");
        }
    }

    @GetMapping(value = "/styles", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> styles() {
        try {
            String response = vietmapService.styles();
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("{\"error\":\"failed to call Vietmap Styles API\",\"detail\":\"" + ex.getMessage() + "\"}");
        }
    }

    @GetMapping(value = "/mobile-styles", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> mobileStyles() {
        try {
            String response = vietmapService.mobileStyles();
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("{\"error\":\"failed to call Vietmap Mobile Styles API\",\"detail\":\"" + ex.getMessage() + "\"}");
        }
    }

    /**
     * Get optimized VietMap style URL for mobile app.
     * Returns a direct URL to VietMap Vector style (includes API key).
     * Mobile SDK will handle caching and optimization automatically.
     * 
     * @return JSON with styleUrl field
     */
    @GetMapping(value = "/mobile-style-url", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getMobileStyleUrl() {
        try {
            String styleUrl = vietmapService.getMobileStyleUrl();
            // Return as JSON object for consistency
            String jsonResponse = String.format("{\"styleUrl\":\"%s\"}", styleUrl);
            return ResponseEntity.ok(jsonResponse);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\":\"failed to generate VietMap style URL\",\"detail\":\"" + ex.getMessage() + "\"}");
        }
    }

    /**
     * Calculate route using Vietmap Route API v3 with full parameter support.
     * Supports vehicle types (car, motorcycle, truck), annotations (congestion data), 
     * alternative routes, and various optimization parameters.
     * 
     * @param request VietmapRouteV3Request containing all route parameters
     * @return VietmapRouteV3Response with route details, instructions, and optional annotations
     * 
     * Example request body:
     * {
     *   "points": ["10.755222,106.662633", "10.7559910,106.6633234"],
     *   "pointsEncoded": true,
     *   "vehicle": "truck",
     *   "capacity": 2000,
     *   "annotations": "congestion,congestion_distance",
     *   "alternative": false
     * }
     */
    @PostMapping(value = "/route-v3", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VietmapRouteV3Response> routeV3(@RequestBody VietmapRouteV3Request request) {
        try {
            VietmapRouteV3Response response = vietmapService.routeV3(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            // Return 400 Bad Request for invalid parameters
            VietmapRouteV3Response errorResponse = VietmapRouteV3Response.builder()
                    .code("INVALID_REQUEST")
                    .messages(ex.getMessage())
                    .build();
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (RuntimeException ex) {
            // Return 502 Bad Gateway for external API errors
            VietmapRouteV3Response errorResponse = VietmapRouteV3Response.builder()
                    .code("ERROR_UNKNOWN")
                    .messages("Failed to call Vietmap Route API v3: " + ex.getMessage())
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(errorResponse);
        }
    }
}
