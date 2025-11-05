package capstone_project.repository.repositories.pricing;

import capstone_project.entity.pricing.SizeRuleEntity;
import capstone_project.repository.repositories.common.BaseRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SizeRuleRepository extends BaseRepository<SizeRuleEntity> {
    Optional<SizeRuleEntity> findByCategoryIdAndVehicleTypeEntityIdAndSizeRuleName(UUID categoryId, UUID vehicleTypeId, String sizeRuleName);

    Optional<SizeRuleEntity> findBysizeRuleName(String sizeRuleName);

    Optional<SizeRuleEntity> findByVehicleTypeEntityId(UUID vehicleTypeId);

    @Query("SELECT v FROM SizeRuleEntity v " +
            "WHERE (v.minWeight IS NULL OR v.minWeight <= :weight) " +
            "AND (v.maxWeight IS NULL OR v.maxWeight >= :weight)")
    List<SizeRuleEntity> findSuitablesizeRules(BigDecimal weight);

    List<SizeRuleEntity> findAllByCategoryId(UUID categoryId);

    List<SizeRuleEntity> findAllByVehicleTypeEntityId(UUID vehicleTypeId);
}
