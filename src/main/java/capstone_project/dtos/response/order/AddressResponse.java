package capstone_project.dtos.response.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressResponse {
    private UUID id;
    private String street;
    private String ward;
    private String province;
    private Boolean addressType;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String fullAddress;
}
