package capstone_project.dtos.response.route;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record RoutePointResponse(
        String name,       // e.g. "Carrier", "Pickup", "Delivery" or custom
        String type,       // semantic type if any
        BigDecimal lat,
        BigDecimal lng,
        String address,
        UUID addressId     // nullable
) {}