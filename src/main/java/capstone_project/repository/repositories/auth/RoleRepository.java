package capstone_project.repository.repositories.auth;

import capstone_project.entity.auth.RoleEntity;
import capstone_project.repository.repositories.common.BaseRepository;

import java.util.Optional;

public interface RoleRepository extends BaseRepository<RoleEntity> {
    Optional<RoleEntity> findByRoleName(String name);
}
