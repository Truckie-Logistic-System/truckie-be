package capstone_project.repository.entityServices.order.transaction;

import capstone_project.entity.order.transaction.TransactionEntity;
import capstone_project.repository.entityServices.common.BaseEntityService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionEntityService extends BaseEntityService<TransactionEntity, UUID> {
    List<TransactionEntity> findByContractId(UUID contractId);

    Optional<TransactionEntity> findByGatewayOrderCode(String gatewayOrderCode);

    boolean existsByContractIdAndStatus(UUID contractId, String status);

    List<TransactionEntity> findByStatusAndCreatedAtBefore(String status, java.time.OffsetDateTime time);
}
