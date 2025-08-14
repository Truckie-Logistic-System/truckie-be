package capstone_project.service.services.user.impl;

import capstone_project.service.entityServices.user.CustomerEntityService;
import capstone_project.service.mapper.user.CustomerMapper;
import capstone_project.service.services.user.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;


@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerEntityService customerEntityService;
    private final CustomerMapper customerMapper;

    @Override
    public void updateCustomerStatus(UUID userId, String status) {
        log.info("Updating customer status for userId: {}, new status: {}", userId, status);

        var customer = customerEntityService.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with ID: " + userId));

        customer.setStatus(status);

        customerEntityService.save(customer);

        log.info("Customer status updated successfully for userId: {}", userId);
    }
}
