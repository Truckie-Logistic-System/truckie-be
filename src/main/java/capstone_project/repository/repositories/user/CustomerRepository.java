package capstone_project.repository.repositories.user;

import capstone_project.entity.user.customer.CustomerEntity;
import capstone_project.repository.repositories.common.BaseRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends BaseRepository<CustomerEntity> {
    Optional<CustomerEntity> findByUserId(UUID id);

    List<CustomerEntity> findByCompanyNameContainingIgnoreCase(String companyName);

    List<CustomerEntity> findByRepresentativeNameContainingIgnoreCase(String name);

    List<CustomerEntity> findByUser_Role_RoleName(String userRoleRoleName);
}
