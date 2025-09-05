package capstone_project.repository.entityServices.pricing;

import capstone_project.entity.pricing.VehicleRuleEntity;
import capstone_project.repository.entityServices.common.BaseEntityService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VehicleRuleEntityService extends BaseEntityService<VehicleRuleEntity, UUID> {
    Optional<VehicleRuleEntity> findByCategoryIdAndVehicleTypeEntityIdAndVehicleRuleName(UUID categoryId, UUID vehicleTypeId, String vehicleRuleName);

    Optional<VehicleRuleEntity> findByVehicleRuleName(String vehicleRuleName);

    Optional<VehicleRuleEntity> findByVehicleTypeId(UUID vehicleTypeId);

    List<VehicleRuleEntity> findSuitableVehicleRules(BigDecimal weight);

    List<VehicleRuleEntity> findAllByCategoryId(UUID categoryId);
}
