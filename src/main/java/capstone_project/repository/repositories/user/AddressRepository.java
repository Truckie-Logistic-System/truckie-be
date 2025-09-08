package capstone_project.repository.repositories.user;

import capstone_project.entity.user.address.AddressEntity;
import capstone_project.repository.repositories.common.BaseRepository;

import java.util.List;
import java.util.UUID;


public interface AddressRepository extends BaseRepository<AddressEntity> {

    List<AddressEntity> findByCustomerId(UUID customerId);
}
