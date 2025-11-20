package capstone_project.service.services.thirdPartyServices.Vietmap;

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
}
