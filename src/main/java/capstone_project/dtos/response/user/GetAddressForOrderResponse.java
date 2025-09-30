package capstone_project.dtos.response.user;

import java.math.BigDecimal;

public record GetAddressForOrderResponse(
        String id,
        String province,
        String ward,
        String street,
        Boolean addressType,
        BigDecimal latitude,
        BigDecimal longitude
) {
}
