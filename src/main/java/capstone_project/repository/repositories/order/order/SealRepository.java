package capstone_project.repository.repositories.order.order;

import capstone_project.entity.order.order.SealEntity;
import capstone_project.repository.repositories.common.BaseRepository;

public interface SealRepository extends BaseRepository<SealEntity> {
    SealEntity findBySealCode(String sealCode);
}
