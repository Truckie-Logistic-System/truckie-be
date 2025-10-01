package capstone_project.dtos.request.route;

import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record SuggestRouteRequest(
        @Size(min = 2, message = "At least two points required")
        List<List<BigDecimal>> points,
        UUID vehicleTypeId
) {}
