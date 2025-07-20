package capstone_project.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Date;


@Entity
@Table(name = "drivers", schema = "public", catalog = "capstone-project")
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class DriversEntity extends BaseEntity {

    @Column(name = "identity_number")
    private String identityNumber;

    @Column(name = "driver_license_number")
    private String driverLicenseNumber;

    @Column(name = "card_serial_number")
    private String cardSerialNumber;

    @Column(name = "place_of_issue")
    private String placeOfIssue;

    @Temporal(TemporalType.DATE)
    @Column(name = "date_of_issue")
    private Date dateOfIssue;

    @Temporal(TemporalType.DATE)
    @Column(name = "date_of_expiry")
    private Date dateOfExpiry;

    @Column(name = "license_class")
    private String licenseClass;

    @Temporal(TemporalType.DATE)
    @Column(name = "date_of_passing")
    private Date dateOfPassing;

    @Column(name = "status")
    private String status;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @JsonBackReference
    private UsersEntity user;
}
