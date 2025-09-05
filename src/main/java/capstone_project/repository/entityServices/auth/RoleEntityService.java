package capstone_project.repository.entityServices.auth;

import capstone_project.entity.auth.RoleEntity;
import capstone_project.repository.entityServices.common.BaseEntityService;

import java.util.Optional;
import java.util.UUID;

public interface RoleEntityService extends BaseEntityService<RoleEntity, UUID> {
    Optional<RoleEntity> findByRoleName(String name);
}
