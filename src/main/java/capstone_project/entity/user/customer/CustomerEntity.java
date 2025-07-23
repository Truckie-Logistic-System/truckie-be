package capstone_project.entity.user.customer;

import capstone_project.entity.common.BaseEntity;
import capstone_project.entity.auth.UserEntity;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "customers", schema = "public", catalog = "capstone-project")
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class CustomerEntity extends BaseEntity {

    @Basic
    @Column(name = "company_name")
    private String companyName;
    @Basic
    @Column(name = "representative_name")
    private String representativeName;
    @Basic
    @Column(name = "representative_phone")
    private String representativePhone;
    @Basic
    @Column(name = "business_license_number")
    private String businessLicenseNumber;
    @Basic
    @Column(name = "business_address")
    private String businessAddress;
    @Basic
    @Column(name = "status")
    private String status;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference
    private UserEntity user;
}
