package capstone_project.repository.entityServices.pricing.impl;

import capstone_project.entity.pricing.SizeRuleEntity;
import capstone_project.repository.entityServices.pricing.SizeRuleEntityService;
import capstone_project.repository.repositories.pricing.SizeRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SizeRuleEntityServiceImpl implements SizeRuleEntityService {

    private final SizeRuleRepository sizeRuleRepository;

    @Override
    public SizeRuleEntity save(SizeRuleEntity entity) {
        return sizeRuleRepository.save(entity);
    }

    @Override
    public Optional<SizeRuleEntity> findEntityById(UUID uuid) {
        return sizeRuleRepository.findById(uuid);
    }

    @Override
    public List<SizeRuleEntity> findAll() {
        return sizeRuleRepository.findAll();
    }

    @Override
    public Optional<SizeRuleEntity> findByCategoryIdAndVehicleTypeEntityIdAndSizeRuleName(UUID categoryId, UUID vehicleTypeId, String sizeRuleName) {
        return sizeRuleRepository.findByCategoryIdAndVehicleTypeEntityIdAndSizeRuleName(categoryId, vehicleTypeId, sizeRuleName);
    }

    @Override
    public Optional<SizeRuleEntity> findBySizeRuleName(String sizeRuleName) {
        return sizeRuleRepository.findBysizeRuleName(sizeRuleName);
    }

    @Override
    public Optional<SizeRuleEntity> findByVehicleTypeId(UUID vehicleTypeId) {
        return sizeRuleRepository.findByVehicleTypeEntityId(vehicleTypeId);
    }

    @Override
    public List<SizeRuleEntity> findSuitableSizeRules(BigDecimal weight) {
        return sizeRuleRepository.findSuitablesizeRules(weight);
    }

    @Override
    public List<SizeRuleEntity> findAllByCategoryId(UUID categoryId) {
        return sizeRuleRepository.findAllByCategoryId(categoryId);
    }
}
