package capstone_project.repository.repositories.pricing;

import capstone_project.entity.pricing.BasingPriceEntity;
import capstone_project.repository.repositories.common.BaseRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BasingPriceRepository extends BaseRepository<BasingPriceEntity> {
    Optional<BasingPriceEntity> findBasingPriceEntityBysizeRuleEntityIdAndDistanceRuleEntityId(UUID sizeRuleEntityId, UUID distanceRuleEntityId);

    List<BasingPriceEntity> findAllBysizeRuleEntityId(UUID sizeRuleEntityId);
}
