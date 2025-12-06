package capstone_project.repository.entityServices.setting;

import capstone_project.entity.setting.CarrierSettingEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CarrierSettingEntityService {
    List<CarrierSettingEntity> findAll();
    Optional<CarrierSettingEntity> findById(UUID id);
    CarrierSettingEntity save(CarrierSettingEntity entity);
    void deleteById(UUID id);
    boolean existsById(UUID id);
}