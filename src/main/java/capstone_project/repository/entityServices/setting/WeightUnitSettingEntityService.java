package capstone_project.repository.entityServices.setting;

import capstone_project.common.enums.WeightUnitEnum;
import capstone_project.entity.setting.WeightUnitSettingEntity;
import capstone_project.repository.entityServices.common.BaseEntityService;

import java.util.Optional;
import java.util.UUID;

public interface WeightUnitSettingEntityService extends BaseEntityService<WeightUnitSettingEntity, UUID> {
    Optional<WeightUnitSettingEntity> getByWeightUnit(String weightUnit);

    Optional<WeightUnitSettingEntity> findByStatus(String status);

    WeightUnitEnum getCurrentUnit();
}
