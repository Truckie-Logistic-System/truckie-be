package capstone_project.controller.user;

import capstone_project.dtos.request.user.UpdateCustomerRequest;
import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.dtos.response.user.CustomerResponse;
import capstone_project.service.services.user.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${customer.api.base-path}")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping("")
    public ResponseEntity<ApiResponse<List<CustomerResponse>>> getAllCustomers() {
        final var cus = customerService.getAllCustomers();
        return ResponseEntity.ok(ApiResponse.ok(cus));
    }

    @GetMapping("/search/role")
    public ResponseEntity<ApiResponse<List<CustomerResponse>>> getAllCustomersByRoleName(@RequestParam String roleName) {
        final var cus = customerService.getAllCustomersByUserRoleName(roleName);
        return ResponseEntity.ok(ApiResponse.ok(cus));
    }

    @GetMapping("search/by-representative-name")
    public ResponseEntity<ApiResponse<List<CustomerResponse>>> getAllCustomersByRepresentativeNameLike(@RequestParam String representativeName) {
        final var cus = customerService.getAllCustomersByRepresentativeNameLike(representativeName);
        return ResponseEntity.ok(ApiResponse.ok(cus));
    }

    @GetMapping("search/by-company-name")
    public ResponseEntity<ApiResponse<List<CustomerResponse>>> getAllCustomersByCompanyNameLike(@RequestParam String companyName) {
        final var cus = customerService.getAllCustomersByCompanyNameLike(companyName);
        return ResponseEntity.ok(ApiResponse.ok(cus));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponse>> getCustomerById(@PathVariable UUID id) {
        final var cus = customerService.getCustomerById(id);
        return ResponseEntity.ok(ApiResponse.ok(cus));
    }

    @GetMapping("/{userId}/user")
    public ResponseEntity<ApiResponse<CustomerResponse>> getCustomerByUserId(@PathVariable UUID userId) {
        final var cus = customerService.getCustomerByUserId(userId);
        return ResponseEntity.ok(ApiResponse.ok(cus));
    }

    @PatchMapping("/{customerId}/status")
    public ResponseEntity<ApiResponse<CustomerResponse>> updateCustomerStatusByCustomerId(@PathVariable UUID customerId, @RequestParam String status) {
        final var cus = customerService.updateCustomerStatusByCustomerId(customerId, status);
        return ResponseEntity.ok(ApiResponse.ok(cus));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponse>> updateCustomer(@PathVariable UUID id, @RequestBody UpdateCustomerRequest updateCustomerRequest) {
        final var cus = customerService.updateCustomer(id, updateCustomerRequest);
        return ResponseEntity.ok(ApiResponse.ok(cus));
    }
}
