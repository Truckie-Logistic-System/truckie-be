package capstone_project.repository.entityServices.user;

import capstone_project.entity.user.address.AddressEntity;
import capstone_project.repository.entityServices.common.BaseEntityService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AddressEntityService extends BaseEntityService<AddressEntity, UUID> {
    List<AddressEntity> getAddressesByCustomerId(UUID customerId);

    /**
     * Find an address by its ID
     * @param id the UUID of the address
     * @return Optional containing the address if found, or empty if not found
     */
    default Optional<AddressEntity> findById(UUID id) {
        return findEntityById(id);
    }
}
