package capstone_project.controller.user;

import capstone_project.dtos.request.user.AddressRequest;
import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.dtos.response.user.AddressResponse;
import capstone_project.service.services.user.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${address.api.base-path}")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class AddressController {
    private final AddressService addressService;

    @PostMapping("")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<ApiResponse<AddressResponse>> createAddress(@RequestBody @Valid AddressRequest addressRequest) {
        final var login = addressService.createAddress(addressRequest);
        return ResponseEntity.ok(ApiResponse.ok(login));
    }

    @GetMapping("")
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getAllAddresses() {
        final var addresses = addressService.getAllAddresses();
        return ResponseEntity.ok(ApiResponse.ok(addresses));
    }

    @GetMapping("/{customerId}/list")
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getAllAddressesByCustomerId(@PathVariable UUID customerId) {
        final var addresses = addressService.getAddressesByCustomerId(customerId);
        return ResponseEntity.ok(ApiResponse.ok(addresses));
    }

    /**
     * Get addresses for the currently authenticated user
     * @return list of addresses for the current user
     */
    @GetMapping("/my-addresses")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getMyAddresses() {
        final var addresses = addressService.getMyAddresses();
        return ResponseEntity.ok(ApiResponse.ok(addresses));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AddressResponse>> getAddressById(@PathVariable UUID id) {
        final var address = addressService.getAddressById(id);
        return ResponseEntity.ok(ApiResponse.ok(address));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(
            @PathVariable UUID id,
            @RequestBody @Valid AddressRequest addressRequest) {
        final var updatedAddress = addressService.updateAddress(id, addressRequest);
        return ResponseEntity.ok(ApiResponse.ok(updatedAddress));
    }
}
