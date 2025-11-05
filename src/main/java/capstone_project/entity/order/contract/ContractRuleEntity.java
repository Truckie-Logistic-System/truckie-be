package capstone_project.entity.order.contract;

import capstone_project.entity.common.BaseEntity;
import capstone_project.entity.order.order.OrderDetailEntity;
import capstone_project.entity.pricing.SizeRuleEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "contract_rules", schema = "public", catalog = "capstone-project")
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ContractRuleEntity extends BaseEntity {

    @Column(name = "num_of_vehicles", length = Integer.MAX_VALUE)
    private Integer numOfVehicles;

    @Column(name = "note")
    @Size(max = 100)
    private String note;

    @Column(name = "status", length = 100)
    private String status;

    @Column(name = "total_value")
    private String info1;

    @Column(name = "info2")
    private String info2;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "size_rule_id")
    private SizeRuleEntity sizeRuleEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    private ContractEntity contractEntity;

//    @ElementCollection
//    @CollectionTable(
//            name = "contract_rule_order_details",
//            joinColumns = @JoinColumn(name = "contract_rule_id")
//    )
//    @Column(name = "order_detail_id")
//    private List<UUID> orderDetails;

    @ManyToMany
    @JoinTable(
            name = "contract_rule_order_detail",
            joinColumns = @JoinColumn(name = "contract_rule_id"),
            inverseJoinColumns = @JoinColumn(name = "order_detail_id")
    )
    @Builder.Default
    private Set<OrderDetailEntity> orderDetails = new HashSet<>();


//    @OneToMany(mappedBy = "contractRuleEntity", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
//    @JsonManagedReference
//    @Builder.Default
//    private List<OrderDetailEntity> orderDetails = new ArrayList<>();
}
