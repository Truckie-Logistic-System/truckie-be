package capstone_project.repository.entityServices.order.contract;

import capstone_project.entity.order.contract.ContractRuleEntity;
import capstone_project.repository.entityServices.common.BaseEntityService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContractRuleEntityService extends BaseEntityService<ContractRuleEntity, UUID> {

    Optional<ContractRuleEntity> findContractRuleEntitiesByContractEntityIdAndVehicleRuleEntityId(UUID vehicleRuleEntityId, UUID contractRuleId);

    Optional<ContractRuleEntity> findEntityById(UUID contractRuleId);

    List<ContractRuleEntity> findContractRuleEntityByContractEntityId(UUID contractId);

    List<UUID> findAssignedOrderDetailIdsByContractRule(UUID contractRuleId);

    void deleteById(UUID id);

    void deleteByContractEntityId(UUID contractId);

    void saveAll(List<ContractRuleEntity> contractRuleEntities);
}
