package capstone_project.repository.entityServices.order.contract.impl;

import capstone_project.entity.order.contract.ContractEntity;
import capstone_project.repository.repositories.order.contract.ContractRepository;
import capstone_project.repository.entityServices.order.contract.ContractEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContractEntityServiceImpl implements ContractEntityService {

    private final ContractRepository contractRepository;

    @Override
    public ContractEntity save(ContractEntity entity) {
        return contractRepository.save(entity);
    }

    @Override
    public Optional<ContractEntity> findEntityById(UUID uuid) {
        return contractRepository.findById(uuid);
    }
    
    @Override
    public Optional<ContractEntity> findByIdWithOrderAndSender(UUID contractId) {
        return contractRepository.findByIdWithOrderAndSender(contractId);
    }

    @Override
    public List<ContractEntity> findAll() {
        return contractRepository.findAll();
    }

    @Override
    public Optional<ContractEntity> getContractByOrderId(UUID orderId) {
        return contractRepository.findContractEntityByOrderEntity_Id(orderId);
    }

    @Override
    public void deleteContractByOrderId(UUID orderId) {
        contractRepository.deleteByOrderEntityId(orderId);
    }

    @Override
    public List<ContractEntity> findByStatusAndSigningDeadlineBefore(String status, LocalDateTime deadline) {
        return contractRepository.findByStatusAndSigningDeadlineBefore(status, deadline);
    }

    @Override
    public List<ContractEntity> findByStatusAndDepositPaymentDeadlineBefore(String status, LocalDateTime deadline) {
        return contractRepository.findByStatusAndDepositPaymentDeadlineBefore(status, deadline);
    }

    @Override
    public List<ContractEntity> findByStatusAndFullPaymentDeadlineBefore(String status, LocalDateTime deadline) {
        return contractRepository.findByStatusAndFullPaymentDeadlineBefore(status, deadline);
    }
}
