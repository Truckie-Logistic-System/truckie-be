package capstone_project.service.entityServices;

import capstone_project.entity.RolesEntity;

import java.util.Optional;

public interface RolesEntityService{
    Optional<RolesEntity> findByRoleName(String name);
}
