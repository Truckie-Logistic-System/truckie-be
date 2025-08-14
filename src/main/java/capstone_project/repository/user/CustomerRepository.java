package capstone_project.repository.user;

import capstone_project.entity.user.customer.CustomerEntity;
import capstone_project.repository.common.BaseRepository;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends BaseRepository<CustomerEntity> {
    Optional<CustomerEntity> findByUserId(UUID id);
}
