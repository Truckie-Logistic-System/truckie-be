package capstone_project.dtos.request.vehicle;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RouteSegmentInfo(
        Integer segmentOrder,
        String startPointName,
        String endPointName,
        BigDecimal startLatitude,
        BigDecimal startLongitude,
        BigDecimal endLatitude,
        BigDecimal endLongitude,
        BigDecimal distanceKilometers,
        List<List<BigDecimal>> pathCoordinates,
        BigDecimal estimatedTollFee,
        List<TollDetail> tollDetails,
        Map<String, Object> rawResponse
) {}