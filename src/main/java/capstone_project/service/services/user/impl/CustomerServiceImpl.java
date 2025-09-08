package capstone_project.service.services.user.impl;

import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.enums.UserStatusEnum;
import capstone_project.common.exceptions.dto.BadRequestException;
import capstone_project.dtos.request.user.UpdateCustomerRequest;
import capstone_project.dtos.response.user.CustomerResponse;
import capstone_project.entity.user.customer.CustomerEntity;
import capstone_project.repository.entityServices.user.CustomerEntityService;
import capstone_project.service.mapper.user.CustomerMapper;
import capstone_project.service.services.user.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;


@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerEntityService customerEntityService;
    private final CustomerMapper customerMapper;

    @Override
    public List<CustomerResponse> getAllCustomers() {
        log.info("Getting all customers");

        List<CustomerEntity> customerEntities = customerEntityService.findAll();
        if (customerEntities.isEmpty()) {
            log.info("No customers found");
            throw new BadRequestException(
                    "No customers found",
                    ErrorEnum.NOT_FOUND.getErrorCode()
            );
        }

        return customerEntities.stream()
                .map(customerMapper::mapCustomerResponse)
                .toList();
    }

    @Override
    public CustomerResponse getCustomerById(UUID id) {
        log.info("Getting customer by ID: {}", id);

        if (id == null) {
            log.error("Customer ID is null");
            throw new BadRequestException(
                    "Customer ID cannot be null",
                    ErrorEnum.INVALID.getErrorCode()
            );
        }

        CustomerEntity customerEntity = customerEntityService.findEntityById(id)
                .orElseThrow(() -> {
                    log.error("Customer not found with ID: {}", id);
                    return new BadRequestException(
                            "Customer not found with ID: " + id,
                            ErrorEnum.NOT_FOUND.getErrorCode()
                    );
                });

        return customerMapper.mapCustomerResponse(customerEntity);
    }

    @Override
    public CustomerResponse getCustomerByUserId(UUID userId) {
        log.info("Getting customer by user ID: {}", userId);

        if (userId == null) {
            log.error("User ID is null");
            throw new BadRequestException(
                    "User ID cannot be null",
                    ErrorEnum.INVALID.getErrorCode()
            );
        }

        CustomerEntity customerEntity = customerEntityService.findByUserId(userId)
                .orElseThrow(() -> {
                    log.error("Customer not found with user ID: {}", userId);
                    return new BadRequestException(
                            "Customer not found with user ID: " + userId,
                            ErrorEnum.NOT_FOUND.getErrorCode()
                    );
                });

        return customerMapper.mapCustomerResponse(customerEntity);
    }

    @Override
    public List<CustomerResponse> getAllCustomersByRepresentativeNameLike(String name) {
        log.info("Getting all customers by name like {}", name);

        if (name == null || name.isBlank()) {
            log.error("Name is null or blank");
            throw new BadRequestException(
                    "Name cannot be null or blank",
                    ErrorEnum.INVALID.getErrorCode()
            );
        }

        List<CustomerEntity> customerEntities = customerEntityService.findAllByRepresentativeNameLike(name);
        if (customerEntities.isEmpty()) {
            log.info("No customers found with name like {}", name);
            throw new BadRequestException(
                    "No customers found with name like: " + name,
                    ErrorEnum.NOT_FOUND.getErrorCode()
            );
        }

        return customerEntities.stream()
                .map(customerMapper::mapCustomerResponse)
                .toList();
    }

    @Override
    public List<CustomerResponse> getAllCustomersByCompanyNameLike(String companyName) {
        log.info("Getting all customers by company name like {}", companyName);

        if (companyName == null || companyName.isBlank()) {
            log.error("Company name is null or blank");
            throw new BadRequestException(
                    "Company name cannot be null or blank",
                    ErrorEnum.INVALID.getErrorCode()
            );
        }

        List<CustomerEntity> customerEntities = customerEntityService.findAllByCompanyNameLike(companyName);
        if (customerEntities.isEmpty()) {
            log.info("No customers found with company name like {}", companyName);
            throw new BadRequestException(
                    "No customers found with company name like: " + companyName,
                    ErrorEnum.NOT_FOUND.getErrorCode()
            );
        }

        return customerEntities.stream()
                .map(customerMapper::mapCustomerResponse)
                .toList();
    }

    @Override
    public List<CustomerResponse> getAllCustomersByUserRoleName(String roleName) {
        log.info("Getting all customers by user role name {}", roleName);

        if (roleName == null || roleName.isBlank()) {
            log.error("Role name is null or blank");
            throw new BadRequestException(
                    "Role name cannot be null or blank",
                    ErrorEnum.INVALID.getErrorCode()
            );
        }

        List<CustomerEntity> customerEntities = customerEntityService.findByUser_Role_RoleName(roleName);
        if (customerEntities.isEmpty()) {
            log.info("No customers found with user role name {}", roleName);
            throw new BadRequestException(
                    "No customers found with user role name: " + roleName,
                    ErrorEnum.NOT_FOUND.getErrorCode()
            );
        }

        return customerEntities.stream()
                .map(customerMapper::mapCustomerResponse)
                .toList();
    }

    @Override
    public void updateCustomerStatus(UUID userId, String status) {
        log.info("Updating customer status for userId: {}, new status: {}", userId, status);

        var customer = customerEntityService.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with ID: " + userId));

        customer.setStatus(status);

        customerEntityService.save(customer);

        log.info("Customer status updated successfully for userId: {}", userId);
    }

    @Override
    public CustomerResponse updateCustomerStatusByCustomerId(UUID customerId, String status) {
        log.info("Updating customer status for customerId: {}, new status: {}", customerId, status);

        if (customerId == null) {
            log.error("Customer ID is null");
            throw new BadRequestException(
                    "Customer ID cannot be null",
                    ErrorEnum.INVALID.getErrorCode()
            );
        }

        CustomerEntity customerEntity = customerEntityService.findEntityById(customerId)
                .orElseThrow(() -> {
                    log.error("Customer not found with ID: {}", customerId);
                    return new BadRequestException(
                            "Customer not found with ID: " + customerId,
                            ErrorEnum.NOT_FOUND.getErrorCode()
                    );
                });

        UserStatusEnum userStatusEnum;
        try {
            userStatusEnum = UserStatusEnum.valueOf(status);
        } catch (IllegalArgumentException | NullPointerException e) {
            log.error("[updateUserStatusById] - Invalid status: {}", status);
            throw new BadRequestException(
                    "Invalid status: " + status,
                    ErrorEnum.ENUM_INVALID.getErrorCode()
            );
        }

        customerEntity.setStatus(userStatusEnum.name());
        CustomerEntity updatedCustomer = customerEntityService.save(customerEntity);

        return customerMapper.mapCustomerResponse(updatedCustomer);
    }

    @Override
    public CustomerResponse updateCustomer(UUID customerId, UpdateCustomerRequest updateCustomerRequest) {
        log.info("Updating customer with ID: {}", customerId);

        if (customerId == null) {
            log.error("Customer ID is null");
            throw new BadRequestException(
                    "Customer ID cannot be null",
                    ErrorEnum.INVALID.getErrorCode()
            );
        }

        CustomerEntity customerEntity = customerEntityService.findEntityById(customerId)
                .orElseThrow(() -> {
                    log.error("Customer not found with ID: {}", customerId);
                    return new BadRequestException(
                            "Customer not found with ID: " + customerId,
                            ErrorEnum.NOT_FOUND.getErrorCode()
                    );
                });

        customerMapper.toCustomerEntity(updateCustomerRequest, customerEntity);
        CustomerEntity updatedCustomer = customerEntityService.save(customerEntity);

        return customerMapper.mapCustomerResponse(updatedCustomer);
    }
}
