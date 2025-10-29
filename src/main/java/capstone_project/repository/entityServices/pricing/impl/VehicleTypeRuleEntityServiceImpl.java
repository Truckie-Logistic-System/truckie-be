package capstone_project.repository.entityServices.pricing.impl;

import capstone_project.entity.pricing.VehicleTypeRuleEntity;
import capstone_project.repository.repositories.pricing.VehicleTypeRuleRepository;
import capstone_project.repository.entityServices.pricing.VehicleTypeRuleEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VehicleTypeRuleEntityServiceImpl implements VehicleTypeRuleEntityService {

    private final VehicleTypeRuleRepository vehicleTypeRuleRepository;

    @Override
    public VehicleTypeRuleEntity save(VehicleTypeRuleEntity entity) {
        return vehicleTypeRuleRepository.save(entity);
    }

    @Override
    public Optional<VehicleTypeRuleEntity> findEntityById(UUID uuid) {
        return vehicleTypeRuleRepository.findById(uuid);
    }

    @Override
    public List<VehicleTypeRuleEntity> findAll() {
        return vehicleTypeRuleRepository.findAll();
    }

    @Override
    public Optional<VehicleTypeRuleEntity> findByCategoryIdAndVehicleTypeEntityIdAndVehicleTypeRuleName(UUID categoryId, UUID vehicleTypeId, String vehicleRuleName) {
        return vehicleTypeRuleRepository.findByCategoryIdAndVehicleTypeEntityIdAndVehicleTypeRuleName(categoryId, vehicleTypeId, vehicleRuleName);
    }

    @Override
    public Optional<VehicleTypeRuleEntity> findByVehicleTypeRuleName(String vehicleRuleName) {
        return vehicleTypeRuleRepository.findByVehicleTypeRuleName(vehicleRuleName);
    }

    @Override
    public Optional<VehicleTypeRuleEntity> findByVehicleTypeId(UUID vehicleTypeId) {
        return vehicleTypeRuleRepository.findByVehicleTypeEntityId(vehicleTypeId);
    }

    @Override
    public List<VehicleTypeRuleEntity> findSuitableVehicleTypeRules(BigDecimal weight) {
        return vehicleTypeRuleRepository.findSuitableVehicleTypeRules(weight);
    }

    @Override
    public List<VehicleTypeRuleEntity> findAllByCategoryId(UUID categoryId) {
        return vehicleTypeRuleRepository.findAllByCategoryId(categoryId);
    }
}
