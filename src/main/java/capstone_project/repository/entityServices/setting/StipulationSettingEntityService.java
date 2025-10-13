package capstone_project.repository.entityServices.setting;

import capstone_project.entity.setting.StipulationSettingEntity;
import capstone_project.repository.entityServices.common.BaseEntityService;

import java.util.Optional;
import java.util.UUID;

public interface StipulationSettingEntityService  extends BaseEntityService<StipulationSettingEntity, UUID> {
    Optional<StipulationSettingEntity> findTopByOrderByIdAsc();

    void deleteById(UUID id);
}
