package capstone_project.entity.user.address;

import capstone_project.entity.common.BaseEntity;
import capstone_project.entity.user.customer.CustomerEntity;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "addresses", schema = "public", catalog = "capstone-project")
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class AddressEntity extends BaseEntity {
    @Size(max = 100)
    @Column(name = "province", length = 20)
    private String province;

    @Size(max = 100)
    @Column(name = "ward", length = 20)
    private String ward;

    @Size(max = 100)
    @Column(name = "street", length = 20)
    private String street;

    @Column(name = "address_type")
    private Boolean addressType;

    @Column(name = "latitude", precision = 11, scale = 8)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 11, scale = 8)
    private BigDecimal longitude;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", insertable = false, updatable = false)
    @JsonBackReference
    private CustomerEntity customer;

}