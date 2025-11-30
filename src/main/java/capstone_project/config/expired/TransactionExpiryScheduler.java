package capstone_project.config.expired;

import capstone_project.common.enums.TransactionEnum;
import capstone_project.entity.order.transaction.TransactionEntity;
import capstone_project.repository.entityServices.order.transaction.TransactionEntityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionExpiryScheduler {
    private final TransactionEntityService transactionEntityService;

    @Scheduled(fixedRate = 600000) // Runs every 10 minutes
    public void cancelExpiredTransactions() {
        OffsetDateTime expiryThreshold = OffsetDateTime.now().minusHours(1);

        List<TransactionEntity> expiredTransactions =
                transactionEntityService.findByStatusAndCreatedAtBefore(
                        TransactionEnum.PENDING.name(), expiryThreshold);

        for (TransactionEntity tx : expiredTransactions) {
            tx.setStatus(TransactionEnum.EXPIRED.name());
            transactionEntityService.save(tx);
            
        }
    }
}
