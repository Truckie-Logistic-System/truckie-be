package capstone_project.repository.entityServices.pricing;

import capstone_project.entity.pricing.VehicleTypeRuleEntity;
import capstone_project.repository.entityServices.common.BaseEntityService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VehicleTypeRuleEntityService extends BaseEntityService<VehicleTypeRuleEntity, UUID> {
    Optional<VehicleTypeRuleEntity> findByCategoryIdAndVehicleTypeEntityIdAndVehicleTypeRuleName(UUID categoryId, UUID vehicleTypeId, String vehicleRuleName);

    Optional<VehicleTypeRuleEntity> findByVehicleTypeRuleName(String vehicleRuleName);

    Optional<VehicleTypeRuleEntity> findByVehicleTypeId(UUID vehicleTypeId);

    List<VehicleTypeRuleEntity> findSuitableVehicleTypeRules(BigDecimal weight);

    List<VehicleTypeRuleEntity> findAllByCategoryId(UUID categoryId);
}
