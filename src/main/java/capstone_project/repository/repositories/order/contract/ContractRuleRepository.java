package capstone_project.repository.repositories.order.contract;

import capstone_project.entity.order.contract.ContractRuleEntity;
import capstone_project.repository.repositories.common.BaseRepository;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContractRuleRepository extends BaseRepository<ContractRuleEntity> {
    Optional<ContractRuleEntity> findContractRuleEntitiesByContractEntityIdAndVehicleTypeRuleEntityId(UUID contractEntity, UUID vehicleRuleEntity);

    Optional<ContractRuleEntity> findContractRuleEntitiesById(UUID id);

    List<ContractRuleEntity> findContractRuleEntityByContractEntityId(UUID contractId);

    @Query(value = """
                SELECT o.id
                FROM order_details o
                JOIN contract_rule_order_detail crod ON crod.order_detail_id = o.id
                WHERE crod.contract_rule_id = :contractRuleId
            """, nativeQuery = true)
    List<UUID> findAssignedOrderDetailIdsByContractRuleId(@Param("contractRuleId") UUID contractRuleId);

    void deleteById(UUID id);

    void deleteByContractEntityId(UUID contractId);

//    @Modifying
//    @Query("DELETE FROM contract_rule_order_detail crd WHERE crd.contract_rule_id = :contractRuleId")
//    void deleteByContractRuleId(@Param("contractRuleId") UUID contractRuleId);
//
//    @Query("SELECT crd.order_detail_id FROM contract_rule_order_detail crd WHERE crd.contract_rule_id = :contractRuleId")
//    List<UUID> findOrderDetailIdsByContractRuleId(@Param("contractRuleId") UUID contractRuleId);

}
