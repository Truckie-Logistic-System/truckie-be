package capstone_project.repository.repositories.setting;

import capstone_project.entity.setting.ContractSettingEntity;
import capstone_project.repository.repositories.common.BaseRepository;

import java.util.Optional;

public interface ContractSettingRepository extends BaseRepository<ContractSettingEntity> {
    Optional<ContractSettingEntity> findFirstByOrderByCreatedAtAsc();
}
