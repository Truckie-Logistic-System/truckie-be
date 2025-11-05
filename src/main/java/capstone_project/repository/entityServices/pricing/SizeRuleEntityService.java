package capstone_project.repository.entityServices.pricing;

import capstone_project.entity.pricing.SizeRuleEntity;
import capstone_project.repository.entityServices.common.BaseEntityService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SizeRuleEntityService extends BaseEntityService<SizeRuleEntity, UUID> {
    Optional<SizeRuleEntity> findByCategoryIdAndVehicleTypeEntityIdAndSizeRuleName(UUID categoryId, UUID vehicleTypeId, String sizeRuleName);

    Optional<SizeRuleEntity> findBySizeRuleName(String sizeRuleName);

    Optional<SizeRuleEntity> findByVehicleTypeId(UUID vehicleTypeId);

    List<SizeRuleEntity> findSuitableSizeRules(BigDecimal weight);

    List<SizeRuleEntity> findAllByCategoryId(UUID categoryId);
}
