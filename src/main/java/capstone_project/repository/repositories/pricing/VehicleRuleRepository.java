package capstone_project.repository.repositories.pricing;

import capstone_project.entity.pricing.VehicleRuleEntity;
import capstone_project.repository.repositories.common.BaseRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VehicleRuleRepository extends BaseRepository<VehicleRuleEntity> {
    Optional<VehicleRuleEntity> findByCategoryIdAndVehicleTypeEntityIdAndVehicleRuleName(UUID categoryId, UUID vehicleTypeId, String vehicleRuleName);

    Optional<VehicleRuleEntity> findByVehicleRuleName(String vehicleRuleName);

    Optional<VehicleRuleEntity> findByVehicleTypeEntityId(UUID vehicleTypeId);

    @Query("SELECT v FROM VehicleRuleEntity v " +
            "WHERE (v.minWeight IS NULL OR v.minWeight <= :weight) " +
            "AND (v.maxWeight IS NULL OR v.maxWeight >= :weight)")
    List<VehicleRuleEntity> findSuitableVehicleRules(BigDecimal weight);

    List<VehicleRuleEntity> findAllByCategoryId(UUID categoryId);
}
