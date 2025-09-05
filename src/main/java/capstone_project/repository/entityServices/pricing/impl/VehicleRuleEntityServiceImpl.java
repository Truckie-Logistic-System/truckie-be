package capstone_project.repository.entityServices.pricing.impl;

import capstone_project.entity.pricing.VehicleRuleEntity;
import capstone_project.repository.repositories.pricing.VehicleRuleRepository;
import capstone_project.repository.entityServices.pricing.VehicleRuleEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VehicleRuleEntityServiceImpl implements VehicleRuleEntityService {

    private final VehicleRuleRepository vehicleRuleRepository;

    @Override
    public VehicleRuleEntity save(VehicleRuleEntity entity) {
        return vehicleRuleRepository.save(entity);
    }

    @Override
    public Optional<VehicleRuleEntity> findEntityById(UUID uuid) {
        return vehicleRuleRepository.findById(uuid);
    }

    @Override
    public List<VehicleRuleEntity> findAll() {
        return vehicleRuleRepository.findAll();
    }

    @Override
    public Optional<VehicleRuleEntity> findByCategoryIdAndVehicleTypeEntityIdAndVehicleRuleName(UUID categoryId, UUID vehicleTypeId, String vehicleRuleName) {
        return vehicleRuleRepository.findByCategoryIdAndVehicleTypeEntityIdAndVehicleRuleName(categoryId, vehicleTypeId, vehicleRuleName);
    }

    @Override
    public Optional<VehicleRuleEntity> findByVehicleRuleName(String vehicleRuleName) {
        return vehicleRuleRepository.findByVehicleRuleName(vehicleRuleName);
    }

    @Override
    public Optional<VehicleRuleEntity> findByVehicleTypeId(UUID vehicleTypeId) {
        return vehicleRuleRepository.findByVehicleTypeEntityId(vehicleTypeId);
    }

    @Override
    public List<VehicleRuleEntity> findSuitableVehicleRules(BigDecimal weight) {
        return vehicleRuleRepository.findSuitableVehicleRules(weight);
    }

    @Override
    public List<VehicleRuleEntity> findAllByCategoryId(UUID categoryId) {
        return vehicleRuleRepository.findAllByCategoryId(categoryId);
    }
}
