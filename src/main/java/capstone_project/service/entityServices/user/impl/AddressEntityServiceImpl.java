package capstone_project.service.entityServices.user.impl;

import capstone_project.entity.order.conformation.SignatureRequestEntity;
import capstone_project.entity.user.address.AddressEntity;
import capstone_project.entity.user.customer.CustomerEntity;
import capstone_project.repository.order.conformation.SignatureRequestRepository;
import capstone_project.repository.user.AddressRepository;
import capstone_project.service.entityServices.user.AddressEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AddressEntityServiceImpl implements AddressEntityService {

    private final AddressRepository addressRepository;

    @Override
    public AddressEntity save(AddressEntity entity) {
        return addressRepository.save(entity);

    }

    @Override
    public Optional<AddressEntity> findEntityById(UUID uuid) {
        return addressRepository.findById(uuid);

    }

    @Override
    public List<AddressEntity> findAll() {
        return addressRepository.findAll();

    }
}
