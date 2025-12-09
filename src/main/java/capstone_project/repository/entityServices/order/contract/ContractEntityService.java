package capstone_project.repository.entityServices.order.contract;

import capstone_project.entity.order.contract.ContractEntity;
import capstone_project.repository.entityServices.common.BaseEntityService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContractEntityService extends BaseEntityService<ContractEntity, UUID> {
    Optional<ContractEntity> getContractByOrderId(UUID orderId);
    
    // Fetch contract with order, sender and user eagerly loaded for PDF generation
    Optional<ContractEntity> findByIdWithOrderAndSender(UUID contractId);

    void deleteContractByOrderId(UUID orderId);

    List<ContractEntity> findByStatusAndSigningDeadlineBefore(String status, LocalDateTime deadline);

    List<ContractEntity> findByStatusAndDepositPaymentDeadlineBefore(String status, LocalDateTime deadline);

    List<ContractEntity> findByStatusAndFullPaymentDeadlineBefore(String status, LocalDateTime deadline);
}
