package capstone_project.entity.setting;

import capstone_project.entity.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "carrier_settings", schema = "public", catalog = "capstone-project")
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class CarrierSettingEntity extends BaseEntity {

    @Size(max = 100)
    @Column(name = "carrier_name", length = 100)
    private String carrierName;

    @Size(max = 200)
    @Column(name = "carrier_address_line", length = 200)
    private String carrierAddressLine;

    @Size(max = 50)
    @Column(name = "carrier_email", length = 50)
    private String carrierEmail;

    @Size(max = 15)
    @Column(name = "carrier_phone", length = 15)
    private String carrierPhone;

    @Size(max = 30)
    @Column(name = "carrier_tax_code", length = 30)
    private String carrierTaxCode;

    @Column(name = "carrier_latitude", precision = 18, scale = 12)
    private BigDecimal carrierLatitude;

    @Column(name = "carrier_longitude", precision = 18, scale = 12)
    private BigDecimal carrierLongitude;
}