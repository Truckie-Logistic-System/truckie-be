package capstone_project.service.services.thirdPartyServices.Vietmap;

import capstone_project.dtos.request.vietmap.VietmapRouteV3Request;
import capstone_project.dtos.response.vietmap.VietmapRouteV3Response;

public interface VietmapService {
    String autocomplete(String text,
                               String focus,
                               Integer cityId,
                               Integer distId,
                               Integer wardId,
                               String circle_center,
                               Integer circle_radius,
                               String cats,
                               String layers);

    String place(String refId);

    String reverse(Double lat, Double lng);

    String routeTolls(String pathJson, Integer vehicle);

    String route(java.util.List<String> points,
                 Boolean pointsEncoded,
                 String vehicle,
                 Boolean optimize,
                 String avoid);

    String styles();

    String mobileStyles();
    
    /**
     * Get optimized Vector style URL for mobile (best performance)
     * Returns: https://maps.vietmap.vn/maps/styles/tm/style.json?apikey={key}
     */
    String getMobileStyleUrl();
    
    /**
     * Calculate route using Vietmap Route API v3 with full parameter support
     * Supports annotations (congestion, congestion_distance), alternative routes, and vehicle types
     * 
     * @param request VietmapRouteV3Request containing all route parameters
     * @return VietmapRouteV3Response with route details, instructions, and annotations
     */
    VietmapRouteV3Response routeV3(VietmapRouteV3Request request);
}
