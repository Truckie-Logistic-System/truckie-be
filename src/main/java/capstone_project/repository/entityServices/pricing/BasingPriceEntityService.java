package capstone_project.repository.entityServices.pricing;

import capstone_project.entity.pricing.BasingPriceEntity;
import capstone_project.repository.entityServices.common.BaseEntityService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BasingPriceEntityService extends BaseEntityService<BasingPriceEntity, UUID> {
    Optional<BasingPriceEntity> findBasingPriceEntityBysizeRuleEntityIdAndDistanceRuleEntityId(UUID sizeRuleEntityId, UUID distanceRuleEntityId);

    List<BasingPriceEntity> findAllBysizeRuleEntityId(UUID sizeRuleEntityId);
}
