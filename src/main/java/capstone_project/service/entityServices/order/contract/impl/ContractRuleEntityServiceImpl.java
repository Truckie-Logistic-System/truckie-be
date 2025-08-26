package capstone_project.service.entityServices.order.contract.impl;

import capstone_project.entity.order.contract.ContractRuleEntity;
import capstone_project.repository.order.contract.ContractRuleRepository;
import capstone_project.service.entityServices.order.contract.ContractRuleEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContractRuleEntityServiceImpl implements ContractRuleEntityService {


    private final ContractRuleRepository contractRuleRepository;

    @Override
    public Optional<ContractRuleEntity> findContractRuleEntitiesByContractEntityIdAndVehicleRuleEntityId(UUID vehicleRuleEntityId, UUID contractRuleId) {
        return contractRuleRepository.findContractRuleEntitiesByContractEntityIdAndVehicleRuleEntityId(vehicleRuleEntityId, contractRuleId);
    }

    @Override
    public List<ContractRuleEntity> findContractRuleEntitiesByContractEntityId(UUID contractRuleId) {
        return  contractRuleRepository.findContractRuleEntitiesByContractEntityId(contractRuleId);
    }

    @Override
    public Optional<ContractRuleEntity> findContractRuleEntityByContractEntityId(UUID contractId) {
        return contractRuleRepository.findContractRuleEntityByContractEntityId(contractId);
    }

    @Override
    public List<UUID> findAssignedOrderDetailIdsByContractRule(UUID contractRuleId) {
        return contractRuleRepository.findAssignedOrderDetailIdsByContractRuleId(contractRuleId);
    }

    @Override
    public void deleteById(UUID id) {
        contractRuleRepository.deleteById(id);
    }

    @Override
    public void deleteByContractEntityId(UUID contractId) {
        contractRuleRepository.deleteByContractEntityId(contractId);
    }

    @Override
    public ContractRuleEntity save(ContractRuleEntity entity) {
        return contractRuleRepository.save(entity);
    }

    @Override
    public Optional<ContractRuleEntity> findById(UUID uuid) {
        return contractRuleRepository.findById(uuid);
    }

    @Override
    public List<ContractRuleEntity> findAll() {
        return contractRuleRepository.findAll();
    }
}
