package capstone_project.service.services.user;

import capstone_project.dtos.request.user.UpdateCustomerRequest;
import capstone_project.dtos.response.user.CustomerResponse;

import java.util.List;
import java.util.UUID;

public interface CustomerService {

    List<CustomerResponse> getAllCustomers();

    CustomerResponse getCustomerById(UUID id);

    CustomerResponse getCustomerByUserId(UUID userId);

    List<CustomerResponse> getAllCustomersByRepresentativeNameLike(String name);

    List<CustomerResponse> getAllCustomersByCompanyNameLike(String companyName);

    List<CustomerResponse> getAllCustomersByUserRoleName(String roleName);

    void updateCustomerStatus(UUID userId, String status);

    CustomerResponse updateCustomerStatusByCustomerId(UUID customerId, String status);

    CustomerResponse updateCustomer(UUID customerId, UpdateCustomerRequest updateCustomerRequest);
}
