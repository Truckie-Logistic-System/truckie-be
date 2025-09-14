package capstone_project.repository.repositories.setting;

import capstone_project.entity.setting.WeightUnitSettingEntity;
import capstone_project.repository.repositories.common.BaseRepository;

import java.util.Optional;

public interface WeightUnitSettingRepository extends BaseRepository<WeightUnitSettingEntity> {
    Optional<WeightUnitSettingEntity> getByWeightUnit(String weightUnit);

    Optional<WeightUnitSettingEntity> findByStatus(String status);
}
