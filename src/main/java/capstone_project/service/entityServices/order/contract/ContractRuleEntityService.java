package capstone_project.service.entityServices.order.contract;

import capstone_project.entity.order.contract.ContractRuleEntity;
import capstone_project.service.entityServices.common.BaseEntityService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContractRuleEntityService extends BaseEntityService<ContractRuleEntity, UUID> {

    Optional<ContractRuleEntity> findContractRuleEntitiesByContractEntityIdAndVehicleRuleEntityId(UUID vehicleRuleEntityId, UUID contractRuleId);

    List<ContractRuleEntity> findContractRuleEntitiesByContractEntityId(UUID contractRuleId);

    Optional<ContractRuleEntity> findContractRuleEntityByContractEntityId(UUID contractId);

    List<UUID> findAssignedOrderDetailIdsByContractRule(UUID contractRuleId);

    void deleteById(UUID id);

    void deleteByContractEntityId(UUID contractId);
}
