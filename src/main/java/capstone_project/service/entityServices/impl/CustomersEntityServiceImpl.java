package capstone_project.service.entityServices.impl;

import capstone_project.entity.CustomersEntity;
import capstone_project.repository.CustomerRepository;
import capstone_project.service.entityServices.CustomersEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomersEntityServiceImpl implements CustomersEntityService {

    private final CustomerRepository customerRepository;

    @Override
    public CustomersEntity createCustomer(CustomersEntity customersEntity) {
        return customerRepository.save(customersEntity);
    }
}

