package capstone_project.repository.entityServices.order.contract;

import capstone_project.entity.order.contract.ContractEntity;
import capstone_project.repository.entityServices.common.BaseEntityService;

import java.util.Optional;
import java.util.UUID;

public interface ContractEntityService extends BaseEntityService<ContractEntity, UUID> {
    Optional<ContractEntity> getContractByOrderId(UUID orderId);
}
