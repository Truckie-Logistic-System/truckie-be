package capstone_project.service.entityServices.impl;

import capstone_project.entity.CustomerEntity;
import capstone_project.repository.CustomerRepository;
import capstone_project.service.entityServices.CustomersEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomersEntityServiceImpl implements CustomersEntityService {

    private final CustomerRepository customerRepository;

//    @Override
//    public CustomerEntity createCustomer(CustomerEntity customerEntity) {
//        return customerRepository.save(customerEntity);
//    }

    @Override
    public CustomerEntity save(CustomerEntity entity) {
        return customerRepository.save(entity);
    }

    @Override
    public Optional<CustomerEntity> findById(UUID uuid) {
        return customerRepository.findById(uuid);
    }

    @Override
    public List<CustomerEntity> findAll() {
        return customerRepository.findAll();
    }
}

