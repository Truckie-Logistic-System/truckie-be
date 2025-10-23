package capstone_project.repository.repositories.order.transaction;

import capstone_project.entity.order.transaction.TransactionEntity;
import capstone_project.repository.repositories.common.BaseRepository;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends BaseRepository<TransactionEntity> {

    List<TransactionEntity> findByContractEntityId(UUID contractEntityId);

    Optional<TransactionEntity> findByGatewayOrderCode(String gatewayOrderCode);

    boolean existsByContractEntityIdAndStatus(UUID contractEntityId, String status);

    List<TransactionEntity> findByStatusAndCreatedAtBefore(String status, LocalDateTime time);

    @Query(value = """
            SELECT SUM(t.amount) 
            FROM "transaction" t 
            WHERE t.contract_id = :contractId AND t.status = 'PAID'
            """, nativeQuery = true)
    BigDecimal sumPaidAmountByContractId(@Param("contractId") UUID contractId);

    @Query(value = """
            SELECT COALESCE(SUM(t.amount), 0)
            FROM transaction t
            WHERE EXTRACT(YEAR FROM t.payment_date) = EXTRACT(YEAR FROM CURRENT_DATE)
              AND t.status = 'PAID';
            """, nativeQuery = true)
    BigDecimal getTotalRevenueInYear();

    @Query(value = """
                SELECT 
                    EXTRACT(YEAR FROM t.payment_date) AS year,
                    COALESCE(SUM(t.amount), 0) AS total
                FROM transaction t
                WHERE EXTRACT(YEAR FROM t.payment_date) IN (EXTRACT(YEAR FROM CURRENT_DATE), EXTRACT(YEAR FROM CURRENT_DATE) - 1)
                  AND t.status = 'PAID'
                GROUP BY EXTRACT(YEAR FROM t.payment_date)
                ORDER BY year;
            """, nativeQuery = true)
    List<Object[]> getTotalRevenueCompareYear();


    @Query(value = """
            SELECT EXTRACT(MONTH FROM t.payment_date) AS month,
                   COALESCE(SUM(t.amount), 0) AS total
            FROM transaction t
            WHERE EXTRACT(YEAR FROM t.payment_date) = EXTRACT(YEAR FROM CURRENT_DATE)
              AND t.status = 'PAID'
            GROUP BY EXTRACT(MONTH FROM t.payment_date)
            ORDER BY month;
            """, nativeQuery = true)
    List<Object[]> getTotalRevenueByMonth();

    @Query(value = """
            SELECT TO_CHAR(t.payment_date, 'IYYY-IW') AS week_label,
                   COALESCE(SUM(t.amount), 0) AS total
            FROM transaction t
            WHERE t.payment_date >= CURRENT_DATE - INTERVAL '28 days'
              AND t.status = 'PAID'
            GROUP BY TO_CHAR(t.payment_date, 'IYYY-IW')
            ORDER BY week_label;
            """, nativeQuery = true)
    List<Object[]> getTotalRevenueByLast4Weeks();
}
