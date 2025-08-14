package capstone_project.service.entityServices.user;

import capstone_project.entity.user.customer.CustomerEntity;
import capstone_project.service.entityServices.common.BaseEntityService;

import java.util.Optional;
import java.util.UUID;

public interface CustomerEntityService extends BaseEntityService<CustomerEntity, UUID> {
    Optional<CustomerEntity> findByUserId(UUID id);
}
