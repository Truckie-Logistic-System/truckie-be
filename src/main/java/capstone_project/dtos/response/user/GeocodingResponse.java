package capstone_project.dtos.response.user;

import java.math.BigDecimal;

public record GeocodingResponse(
        BigDecimal latitude,
        BigDecimal longitude,
        String province,
        String ward,
        String street
) {}
