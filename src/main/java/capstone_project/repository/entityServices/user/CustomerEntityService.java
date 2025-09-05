package capstone_project.repository.entityServices.user;

import capstone_project.entity.user.customer.CustomerEntity;
import capstone_project.repository.entityServices.common.BaseEntityService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerEntityService extends BaseEntityService<CustomerEntity, UUID> {
    Optional<CustomerEntity> findByUserId(UUID id);

    List<CustomerEntity> findAllByRepresentativeNameLike(String name);

    List<CustomerEntity> findAllByCompanyNameLike(String companyName);

    List<CustomerEntity> findByUser_Role_RoleName(String userRoleRoleName);
}
