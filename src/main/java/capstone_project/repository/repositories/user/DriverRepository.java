package capstone_project.repository.repositories.user;

import capstone_project.entity.user.driver.DriverEntity;
import capstone_project.repository.repositories.common.BaseRepository;

import java.util.List;

public interface DriverRepository extends BaseRepository<DriverEntity> {
    List<DriverEntity> findByUser_Role_RoleName(String userRoleRoleName);

}
