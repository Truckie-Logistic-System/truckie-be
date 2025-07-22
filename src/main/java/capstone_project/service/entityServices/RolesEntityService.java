package capstone_project.service.entityServices;

import capstone_project.entity.RolesEntity;

import java.util.Optional;
import java.util.UUID;

public interface RolesEntityService extends BaseEntityService<RolesEntity, UUID> {
    Optional<RolesEntity> findByRoleName(String name);
}
