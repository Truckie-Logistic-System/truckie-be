package capstone_project.entity.order.order;


import capstone_project.entity.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "seals", schema = "public", catalog = "capstone-project")
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class SealEntity extends BaseEntity {

    @Column(name = "seal_code")
    private String sealCode;

    @Column(name = "description")
    private String description;

    @Size(max = 20)
    @Column(name = "status", length = 20)
    private String status;
}
