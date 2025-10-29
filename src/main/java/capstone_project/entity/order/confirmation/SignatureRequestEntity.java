package capstone_project.entity.order.confirmation;

import capstone_project.entity.auth.UserEntity;
import capstone_project.entity.common.BaseEntity;
import capstone_project.entity.order.order.OrderEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "signature_requests", schema = "public", catalog = "capstone-project")
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class SignatureRequestEntity extends BaseEntity {
    @Column(name = "signature_image_url", length = Integer.MAX_VALUE)
    private String signatureImageUrl;

    @Size(max = 20)
    @Column(name = "status", length = 20)
    private String status;

    @Size(max = 200)
    @Column(name = "notes", length = 200)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private OrderEntity orderEntity;

}
