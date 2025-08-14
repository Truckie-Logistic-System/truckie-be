package capstone_project.dtos.response.user;

import java.io.Serializable;
import java.math.BigDecimal;

public record AddressResponse(
        String id,
        String province,
        String ward,
        String street,
        Boolean addressType,
        BigDecimal latitude,
        BigDecimal longitude,
        String customerId
) {}