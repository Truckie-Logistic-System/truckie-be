package capstone_project.dtos.response.route;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record RouteSegmentResponse(
        int segmentOrder,
        String startName,
        String endName,
        BigDecimal startLat,                   // start point latitude
        BigDecimal startLng,                   // start point longitude
        BigDecimal endLat,                     // end point latitude
        BigDecimal endLng,                     // end point longitude
        List<List<BigDecimal>> path,          // list of [lng, lat]
        List<TollResponse> tolls,
        double distance,                       // in kilometers
        Map<String, Object> rawResponse  // optional raw vietmap response
) {}
