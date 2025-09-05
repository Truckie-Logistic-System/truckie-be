package capstone_project.repository.repositories.order.contract;

import capstone_project.entity.order.contract.ContractEntity;
import capstone_project.repository.repositories.common.BaseRepository;

import java.util.Optional;
import java.util.UUID;

public interface ContractRepository extends BaseRepository<ContractEntity> {
    Optional<ContractEntity> getContractByOrderEntityId(UUID orderId);
}
