package capstone_project.dtos.request.vehicle;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RouteInfo(
        List<RouteSegmentInfo> segments,
        BigDecimal totalTollFee,
        Integer totalTollCount,
        BigDecimal totalDistance
) {}
