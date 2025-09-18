package capstone_project.repository.entityServices.setting;

import capstone_project.entity.setting.ContractSettingEntity;
import capstone_project.repository.entityServices.common.BaseEntityService;

import java.util.Optional;
import java.util.UUID;

public interface ContractSettingEntityService extends BaseEntityService<ContractSettingEntity, UUID> {
    Optional<ContractSettingEntity> findFirstByOrderByCreatedAtAsc();
}
