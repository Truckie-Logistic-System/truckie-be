package capstone_project.repository.entityServices.setting.impl;

import capstone_project.common.enums.WeightUnitEnum;
import capstone_project.entity.setting.WeightUnitSettingEntity;
import capstone_project.repository.entityServices.setting.WeightUnitSettingEntityService;
import capstone_project.repository.repositories.setting.WeightUnitSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeightUnitSettingEntityServiceImpl implements WeightUnitSettingEntityService {

    private final WeightUnitSettingRepository weightUnitSettingRepository;

    @Override
    public WeightUnitSettingEntity save(WeightUnitSettingEntity entity) {
        return weightUnitSettingRepository.save(entity);
    }

    @Override
    public Optional<WeightUnitSettingEntity> findEntityById(UUID uuid) {
        return weightUnitSettingRepository.findById(uuid);
    }

    @Override
    public List<WeightUnitSettingEntity> findAll() {
        return weightUnitSettingRepository.findAll();
    }

    @Override
    public Optional<WeightUnitSettingEntity> getByWeightUnit(String weightUnit) {
        return weightUnitSettingRepository.getByWeightUnit(weightUnit);
    }

    @Override
    public Optional<WeightUnitSettingEntity> findByStatus(String status) {
        return weightUnitSettingRepository.findByStatus(status);
    }

    @Override
    public WeightUnitEnum getCurrentUnit() {
        return weightUnitSettingRepository.findByStatus("ACTIVE")
                .map(setting -> WeightUnitEnum.valueOf(setting.getWeightUnit().toUpperCase()))
                .orElse(WeightUnitEnum.KG);
    }
}
