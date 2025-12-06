package capstone_project.dtos.request.setting;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarrierSettingRequest {
    @Size(max = 255)
    private String carrierName;

    @Size(max = 255)
    private String representativeName;

    @Size(max = 500)
    private String carrierAddressLine;

    @Email
    private String carrierEmail;

    @Size(max = 50)
    private String carrierPhone;

    @Size(max = 100)
    private String carrierTaxCode;

    private BigDecimal carrierLatitude;
    private BigDecimal carrierLongitude;
}
