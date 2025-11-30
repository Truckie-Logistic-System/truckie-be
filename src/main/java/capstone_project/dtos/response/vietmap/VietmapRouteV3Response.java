package capstone_project.dtos.response.vietmap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for Vietmap Route API v3
 * Contains route information including distance, time, instructions, and optional annotations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class VietmapRouteV3Response {
    
    /**
     * License type associated with the routing data
     */
    @JsonProperty("license")
    private String license;
    
    /**
     * Status code indicating the success or failure of the request
     * Values: OK, INVALID_REQUEST, OVER_DAILY_LIMIT, MAX_POINTS_EXCEED, ERROR_UNKNOWN, ZERO_RESULTS
     */
    @JsonProperty("code")
    private String code;
    
    /**
     * Error message (singular) - used when API returns error
     */
    @JsonProperty("message")
    private String message;
    
    /**
     * Additional messages (plural) - used in success response (if any)
     */
    @JsonProperty("messages")
    private String messages;
    
    /**
     * Array containing route information including distance, time, etc.
     */
    @JsonProperty("paths")
    private List<RoutePath> paths;
    
    /**
     * Represents a single route path with all its details
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RoutePath {
        
        /**
         * Total distance of the route (in meters)
         */
        @JsonProperty("distance")
        private Double distance;
        
        /**
         * Weight assigned to the route
         */
        @JsonProperty("weight")
        private Double weight;
        
        /**
         * Total time required for the route (in milliseconds)
         */
        @JsonProperty("time")
        private Long time;
        
        /**
         * Number of transfers required for the route
         */
        @JsonProperty("transfers")
        private Integer transfers;
        
        /**
         * Number of transfer (singular) - used in some API responses
         */
        @JsonProperty("transfer")
        private Integer transfer;
        
        /**
         * Whether the points and snapped_waypoints fields are polyline-encoded strings
         */
        @JsonProperty("points_encoded")
        private Boolean pointsEncoded;
        
        /**
         * The bounding box of the route geometry
         * Format: [minLon, minLat, maxLon, maxLat]
         */
        @JsonProperty("bbox")
        private List<Double> bbox;
        
        /**
         * Encoded points representing the route using google polyline 5 format
         * If pointsEncoded is false, this will be a list of [lat,lng] coordinates
         */
        @JsonProperty("points")
        private Object points; // Can be String (encoded) or List<List<Double>> (decoded)
        
        /**
         * Array containing turn-by-turn navigation instructions
         */
        @JsonProperty("instructions")
        private List<RouteInstruction> instructions;
        
        /**
         * Snapped waypoints representing the route
         * If pointsEncoded is true, this is an encoded string
         * If pointsEncoded is false, this is a list of [lat,lng] coordinates
         */
        @JsonProperty("snapped_waypoints")
        private Object snappedWaypoints; // Can be String (encoded) or List<List<Double>> (decoded)
        
        /**
         * Array of annotation objects to calculate speed, congestion
         * Only present if annotations parameter was requested
         */
        @JsonProperty("annotations")
        private RouteAnnotations annotations;
    }
    
    /**
     * Turn-by-turn navigation instruction
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RouteInstruction {
        
        /**
         * Distance until the next instruction (in meters)
         */
        @JsonProperty("distance")
        private Double distance;
        
        /**
         * Heading direction of the instruction (if available)
         */
        @JsonProperty("heading")
        private Integer heading;
        
        /**
         * Direction sign of the instruction
         */
        @JsonProperty("sign")
        private Integer sign;
        
        /**
         * Two indices into points, referring to the beginning and end of the segment
         */
        @JsonProperty("interval")
        private List<Integer> interval;
        
        /**
         * A description what the user has to do in order to follow the route
         */
        @JsonProperty("text")
        private String text;
        
        /**
         * The duration for this instruction, in milliseconds
         */
        @JsonProperty("time")
        private Long time;
        
        /**
         * The name of the street to turn onto in order to follow the route
         */
        @JsonProperty("street_name")
        private String streetName;
        
        /**
         * Last heading direction of the instruction (if available)
         */
        @JsonProperty("last_heading")
        private Integer lastHeading;
    }
    
    /**
     * Route annotations containing congestion and distance information
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RouteAnnotations {
        
        /**
         * Array of congestion levels for segments
         * Possible values: low, moderate, heavy, severe, unknown
         * Based on average speed (km/h):
         * - low: ≥ 40
         * - moderate: 20–<40
         * - heavy: 10–<20
         * - severe: >0–≤10
         */
        @JsonProperty("congestion")
        private List<String> congestion;
        
        /**
         * Array of distances (in meters) that only includes segments where congestion level is ≥ heavy
         */
        @JsonProperty("congestion_distance")
        private List<Double> congestionDistance;
    }
}
