package capstone_project.repository.repositories.order.transaction;

import capstone_project.entity.order.transaction.TransactionEntity;
import capstone_project.repository.repositories.common.BaseRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends BaseRepository<TransactionEntity> {

    List<TransactionEntity> findByContractEntityId(UUID contractEntityId);

    Optional<TransactionEntity> findByGatewayOrderCode(Long gatewayOrderCode);

    boolean existsByContractEntityIdAndStatus(UUID contractEntityId, String status);

    List<TransactionEntity> findByStatusAndCreatedAtBefore(String status, LocalDateTime time);
}
