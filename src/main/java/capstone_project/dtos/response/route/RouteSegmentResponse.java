package capstone_project.dtos.response.route;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record RouteSegmentResponse(
        int segmentOrder,
        String startName,
        String endName,
        List<List<BigDecimal>> path,          // list of [lng, lat]
        List<TollResponse> tolls,
        Map<String, Object> rawResponse  // optional raw vietmap response
) {}
