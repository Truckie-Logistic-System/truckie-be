package capstone_project.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;


@Entity
@Table(name = "drivers", schema = "public", catalog = "capstone-project")
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class DriverEntity extends BaseEntity {

    @Column(name = "identity_number")
    private String identityNumber;

    @Column(name = "driver_license_number")
    private String driverLicenseNumber;

    @Column(name = "card_serial_number")
    private String cardSerialNumber;

    @Column(name = "place_of_issue")
    private String placeOfIssue;

    @Column(name = "date_of_issue")
    private LocalDateTime dateOfIssue;

    @Column(name = "date_of_expiry")
    private LocalDateTime dateOfExpiry;

    @Column(name = "license_class")
    private String licenseClass;

    @Column(name = "date_of_passing")
    private LocalDateTime dateOfPassing;

    @Column(name = "status")
    private String status;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @JsonBackReference
    private UsersEntity user;
}
