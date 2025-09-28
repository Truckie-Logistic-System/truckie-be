package capstone_project.dtos.request.user;

import java.math.BigDecimal;

public record AddressRequest(
        String street,
        String ward,
        String province,
        Boolean addressType,
        BigDecimal latitude,
        BigDecimal longitude
) {
}
