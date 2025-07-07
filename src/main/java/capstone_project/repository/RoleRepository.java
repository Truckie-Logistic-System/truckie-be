package capstone_project.repository;

import capstone_project.entity.RolesEntity;

import java.util.Optional;

public interface RoleRepository extends BaseRepository<RolesEntity> {
    Optional<RolesEntity> findByRoleName(String name);
}
