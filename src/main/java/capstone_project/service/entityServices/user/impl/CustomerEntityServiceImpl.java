package capstone_project.service.entityServices.user.impl;

import capstone_project.entity.user.customer.CustomerEntity;
import capstone_project.repository.user.CustomerRepository;
import capstone_project.service.entityServices.user.CustomerEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerEntityServiceImpl implements CustomerEntityService {

    private final CustomerRepository customerRepository;

    @Override
    public Optional<CustomerEntity> findByUserId(UUID id) {
        return customerRepository.findByUserId(id);
    }

//    @Override
//    public CustomerEntity createCustomer(CustomerEntity customerEntity) {
//        return customerRepository.save(customerEntity);
//    }

    @Override
    public CustomerEntity save(CustomerEntity entity) {
        return customerRepository.save(entity);
    }

    @Override
    public Optional<CustomerEntity> findContractRuleEntitiesById(UUID uuid) {
        return customerRepository.findById(uuid);
    }

    @Override
    public List<CustomerEntity> findAll() {
        return customerRepository.findAll();
    }
}

