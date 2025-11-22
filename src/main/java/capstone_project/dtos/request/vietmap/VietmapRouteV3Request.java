package capstone_project.dtos.request.vietmap;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for Vietmap Route API v3
 * Supports route calculation with multiple waypoints, vehicle types, and annotations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VietmapRouteV3Request {
    
    /**
     * The points for which the route should be calculated.
     * Format: [latitude,longitude]
     * Specify at least an origin and a destination. Via points are possible.
     * Required: Yes
     */
    private List<String> points;
    
    /**
     * Allows changing the encoding of location data in the response.
     * Default is polyline encoding (true)
     * Set to false to switch to simple coordinate pairs like [lon,lat]
     * Default: true
     */
    private Boolean pointsEncoded;
    
    /**
     * Config the vehicle profiles for which the route should be calculated.
     * Enum: car, motorcycle, truck
     * Default: car
     */
    private String vehicle;
    
    /**
     * Execute TSP if the value = true
     * Under normal case, please do not enable this parameter
     * Default: false
     */
    private Boolean optimize;
    
    /**
     * Required only when vehicle=truck, represent for the truck's weight in kilogram
     * Conditional: Required if vehicle=truck
     */
    private Integer capacity;
    
    /**
     * Departure time
     * Format: ISO-8601 date representation (UTC 0)
     * Example: 2025-08-01T12:01:00Z
     */
    private String time;
    
    /**
     * Return any alternative route or not
     * Default: false
     */
    private Boolean alternative;
    
    /**
     * Current heading of vehicle (in degrees)
     */
    private Double heading;
    
    /**
     * Enables per-segment metadata for the route.
     * Provide a comma-separated list of keys.
     * Allowed values: congestion, congestion_distance
     * Example: "congestion,congestion_distance"
     * Note: Requesting annotations increases compute time and payload size
     */
    private String annotations;
    
    /**
     * Avoid a specific road types, such as ferry
     */
    private String avoid;
}
