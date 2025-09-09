package capstone_project.repository.entityServices.order.transaction.Impl;

import capstone_project.entity.order.transaction.TransactionEntity;
import capstone_project.repository.repositories.order.transaction.TransactionRepository;
import capstone_project.repository.entityServices.order.transaction.TransactionEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionEntityServiceImpl implements TransactionEntityService {

    private final TransactionRepository transactionRepository;

    @Override
    public TransactionEntity save(TransactionEntity entity) {
        return transactionRepository.save(entity);
    }

    @Override
    public Optional<TransactionEntity> findEntityById(UUID uuid) {
        return transactionRepository.findById(uuid);
    }

    @Override
    public List<TransactionEntity> findAll() {
        return transactionRepository.findAll();
    }

    @Override
    public List<TransactionEntity> findByContractId(UUID contractId) {
        return transactionRepository.findByContractEntityId(contractId);
    }

    @Override
    public Optional<TransactionEntity> findByGatewayOrderCode(Long gatewayOrderCode) {
        return transactionRepository.findByGatewayOrderCode(gatewayOrderCode);
    }

    @Override
    public boolean existsByContractIdAndStatus(UUID contractId, String status) {
        return transactionRepository.existsByContractEntityIdAndStatus(contractId, status);
    }

    @Override
    public List<TransactionEntity> findByStatusAndCreatedAtBefore(String status, OffsetDateTime time) {
        return transactionRepository.findByStatusAndCreatedAtBefore(status, time.toLocalDateTime());
    }
}
