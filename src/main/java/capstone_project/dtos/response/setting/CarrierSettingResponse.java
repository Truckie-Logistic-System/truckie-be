package capstone_project.dtos.response.setting;

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
public class CarrierSettingResponse {
    private UUID id;
    private String carrierName;
    private String representativeName;
    private String carrierAddressLine;
    private String carrierEmail;
    private String carrierPhone;
    private String carrierTaxCode;
    private BigDecimal carrierLatitude;
    private BigDecimal carrierLongitude;
}
