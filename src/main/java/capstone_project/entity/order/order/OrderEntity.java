package capstone_project.entity.order.order;

import capstone_project.entity.user.address.AddressEntity;
import capstone_project.entity.common.BaseEntity;
import capstone_project.entity.user.customer.CustomerEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders", schema = "public", catalog = "capstone-project")
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class OrderEntity extends BaseEntity {
    @Size(max = 100)
    @Column(name = "notes", length = 100)
    private String notes;

    @Column(name = "total_quantity")
    private Integer totalQuantity;

//    @Column(name = "total_weight")
//    private BigDecimal totalWeight;

    @Size(max = 100)
    @Column(name = "order_code", length = 100)
    private String orderCode;

    @Size(max = 100)
    @Column(name = "receiver_identity", length = 100)
    private String receiverIdentity;

    @Size(max = 100)
    @Column(name = "receiver_name", length = 100)
    private String receiverName;

    @Size(max = 20)
    @Column(name = "receiver_phone", length = 20)
    private String receiverPhone;

    @Size(max = 20)
    @Column(name = "status", length = 20)
    private String status;

    @Size(max = 200)
    @Column(name = "package_description", length = 200)
    private String packageDescription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_address_id")
    private AddressEntity deliveryAddress;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pickup_address_id")
    private AddressEntity pickupAddress;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private CustomerEntity sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private CategoryEntity category;

    @OneToMany(mappedBy = "orderEntity", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<OrderDetailEntity> orderDetailEntities = new ArrayList<>();

}