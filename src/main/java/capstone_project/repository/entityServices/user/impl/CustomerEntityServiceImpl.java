package capstone_project.repository.entityServices.user.impl;

import capstone_project.entity.user.customer.CustomerEntity;
import capstone_project.repository.repositories.user.CustomerRepository;
import capstone_project.repository.entityServices.user.CustomerEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
public class CustomerEntityServiceImpl implements CustomerEntityService {

    private final CustomerRepository customerRepository;

    @Override
    public Optional<CustomerEntity> findByUserId(UUID id) {
        return customerRepository.findByUserId(id);
    }

    @Override
    public List<CustomerEntity> findAllByRepresentativeNameLike(String name) {
        List<CustomerEntity> customers = customerRepository.findByRepresentativeNameContainingIgnoreCase(name);
        // Sort by createdAt descending (newest first)
        customers.sort(Comparator.comparing(CustomerEntity::getCreatedAt).reversed());
        return customers;
    }

    @Override
    public List<CustomerEntity> findAllByCompanyNameLike(String companyName) {
        List<CustomerEntity> customers = customerRepository.findByCompanyNameContainingIgnoreCase(companyName);
        // Sort by createdAt descending (newest first)
        customers.sort(Comparator.comparing(CustomerEntity::getCreatedAt).reversed());
        return customers;
    }

    @Override
    public List<CustomerEntity> findByUser_Role_RoleName(String userRoleRoleName) {
        List<CustomerEntity> customers = customerRepository.findByUser_Role_RoleName(userRoleRoleName);
        // Sort by createdAt descending (newest first)
        customers.sort(Comparator.comparing(CustomerEntity::getCreatedAt).reversed());
        return customers;
    }

    @Override
    public List<Object[]> newCustomerByMonthOverYear(int year) {
        return customerRepository.newCustomerByMonthOverYear(year);
    }

    @Override
    public List<Object[]> getUserGrowthRateByYear(int year) {
        return customerRepository.getCustomerGrowthRateByYear(year);
    }

    @Override
    public List<Object[]> getTopCustomersByRevenue(int amount) {
        return customerRepository.getTopCustomersByRevenue(amount);
    }

    @Override
    public CustomerEntity save(CustomerEntity entity) {
        return customerRepository.save(entity);
    }

    @Override
    public Optional<CustomerEntity> findEntityById(UUID uuid) {
        return customerRepository.findById(uuid);
    }

    @Override
    public List<CustomerEntity> findAll() {
        List<CustomerEntity> customers = customerRepository.findAll();
        // Sort by createdAt descending (newest first)
        customers.sort(Comparator.comparing(CustomerEntity::getCreatedAt).reversed());
        return customers;
    }


}

