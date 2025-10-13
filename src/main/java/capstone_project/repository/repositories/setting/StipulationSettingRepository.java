package capstone_project.repository.repositories.setting;

import capstone_project.entity.setting.StipulationSettingEntity;
import capstone_project.repository.repositories.common.BaseRepository;

import java.util.Optional;

public interface StipulationSettingRepository extends BaseRepository<StipulationSettingEntity> {
    Optional<StipulationSettingEntity> findTopByOrderByIdAsc();
}
