package capstone_project.repository.repositories.setting;

import capstone_project.entity.setting.CarrierSettingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CarrierSettingRepository extends JpaRepository<CarrierSettingEntity, UUID> {
    Optional<CarrierSettingEntity> findTopByOrderByIdAsc();
}