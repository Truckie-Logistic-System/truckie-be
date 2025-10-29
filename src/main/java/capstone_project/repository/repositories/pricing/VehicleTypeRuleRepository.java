package capstone_project.repository.repositories.pricing;

import capstone_project.entity.pricing.VehicleTypeRuleEntity;
import capstone_project.repository.repositories.common.BaseRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VehicleTypeRuleRepository extends BaseRepository<VehicleTypeRuleEntity> {
    Optional<VehicleTypeRuleEntity> findByCategoryIdAndVehicleTypeEntityIdAndVehicleTypeRuleName(UUID categoryId, UUID vehicleTypeId, String vehicleRuleName);

    Optional<VehicleTypeRuleEntity> findByVehicleTypeRuleName(String vehicleRuleName);

    Optional<VehicleTypeRuleEntity> findByVehicleTypeEntityId(UUID vehicleTypeId);

    @Query("SELECT v FROM VehicleTypeRuleEntity v " +
            "WHERE (v.minWeight IS NULL OR v.minWeight <= :weight) " +
            "AND (v.maxWeight IS NULL OR v.maxWeight >= :weight)")
    List<VehicleTypeRuleEntity> findSuitableVehicleTypeRules(BigDecimal weight);

    List<VehicleTypeRuleEntity> findAllByCategoryId(UUID categoryId);

    List<VehicleTypeRuleEntity> findAllByVehicleTypeEntityId(UUID vehicleTypeId);
}
