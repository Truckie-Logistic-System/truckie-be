package capstone_project.repository.entityServices.user;

import capstone_project.entity.user.driver.DriverEntity;
import capstone_project.repository.entityServices.common.BaseEntityService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DriverEntityService extends BaseEntityService<DriverEntity, UUID> {
    List<DriverEntity> findByUser_Role_RoleName(String userRoleRoleName);

    Optional<DriverEntity> findByUserId(UUID userId);
}
