package capstone_project.repository.repositories.order.contract;

import capstone_project.entity.order.contract.ContractEntity;
import capstone_project.repository.repositories.common.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContractRepository extends BaseRepository<ContractEntity> {
    Optional<ContractEntity> findContractEntityByOrderEntity_Id(UUID orderEntityId);
    
    // Fetch contract with order, sender (customer) and user eagerly loaded for PDF generation
    @Query("SELECT c FROM ContractEntity c " +
           "LEFT JOIN FETCH c.orderEntity o " +
           "LEFT JOIN FETCH o.sender s " +
           "LEFT JOIN FETCH s.user " +
           "WHERE c.id = :contractId")
    Optional<ContractEntity> findByIdWithOrderAndSender(@Param("contractId") UUID contractId);

    void deleteByOrderEntityId(UUID orderId);

    // Find contracts with expired signing deadline (CONTRACT_DRAFT status and signing_deadline < now)
    List<ContractEntity> findByStatusAndSigningDeadlineBefore(String status, LocalDateTime deadline);

    // Find contracts with expired deposit payment deadline (CONTRACT_SIGNED status and deposit_payment_deadline < now)
    List<ContractEntity> findByStatusAndDepositPaymentDeadlineBefore(String status, LocalDateTime deadline);

    // Find contracts with expired full payment deadline (DEPOSITED status and full_payment_deadline < now)
    List<ContractEntity> findByStatusAndFullPaymentDeadlineBefore(String status, LocalDateTime deadline);
}
