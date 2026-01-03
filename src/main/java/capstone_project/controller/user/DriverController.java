package capstone_project.controller.user;

import capstone_project.dtos.request.user.BulkDriverGenerationRequest;
import capstone_project.dtos.request.user.LicenseRenewalRequest;
import capstone_project.dtos.request.user.UpdateDriverRequest;
import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.dtos.response.user.DriverResponse;
import capstone_project.service.services.user.DriverService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${driver.api.base-path}")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class DriverController {

    private final DriverService driverService;

    @GetMapping("")
    public ResponseEntity<ApiResponse<List<DriverResponse>>> getAllDrivers() {
        final var driver = driverService.getAllDrivers();
        return ResponseEntity.ok(ApiResponse.ok(driver));
    }

    @GetMapping("/search/role")
    public ResponseEntity<ApiResponse<List<DriverResponse>>> getAllDriversByRoleName(@RequestParam String roleName) {
        final var cus = driverService.getAllDriversByUserRoleName(roleName);
        return ResponseEntity.ok(ApiResponse.ok(cus));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DriverResponse>> getDriverById(@PathVariable UUID id) {
        final var driver = driverService.getDriverById(id);
        return ResponseEntity.ok(ApiResponse.ok(driver));
    }

    @GetMapping("/user")
    public ResponseEntity<ApiResponse<DriverResponse>> getDriverByUserId() {
        final var driver = driverService.getDriverByUserId();
        return ResponseEntity.ok(ApiResponse.ok(driver));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<DriverResponse>> updateDriverStatus(@PathVariable UUID id, @RequestParam String status) {
        final var driver = driverService.updateDriverStatus(id, status);
        return ResponseEntity.ok(ApiResponse.ok(driver));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DriverResponse>> updateDriver(@PathVariable UUID id, @RequestBody UpdateDriverRequest updateDriverRequest) {
        final var driver = driverService.updateDriver(id, updateDriverRequest);
        return ResponseEntity.ok(ApiResponse.ok(driver));
    }

    @PostMapping("/generate-bulk")
    @PreAuthorize("hasRole('ADMIN')")
    @Hidden
    public ResponseEntity<ApiResponse<List<DriverResponse>>> generateBulkDrivers(@RequestBody BulkDriverGenerationRequest request) {
        final var drivers = driverService.generateBulkDrivers(request.getCount());
        return ResponseEntity.ok(ApiResponse.ok(drivers));
    }

    @GetMapping("/validate-by-phone/{phoneNumber}")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<DriverResponse>> validateDriverByPhone(@PathVariable String phoneNumber) {
        final var driver = driverService.validateDriverByPhone(phoneNumber);
        return ResponseEntity.ok(ApiResponse.ok(driver));
    }

    @PutMapping("/{id}/renew-license")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<DriverResponse>> renewDriverLicense(
            @PathVariable UUID id,
            @RequestBody LicenseRenewalRequest request) {
        final var driver = driverService.renewDriverLicense(id, request);
        return ResponseEntity.ok(ApiResponse.ok(driver));
    }

    @PostMapping("/update-realistic-data")
    @PreAuthorize("hasRole('ADMIN')")
    @Hidden
    public ResponseEntity<ApiResponse<List<DriverResponse>>> updateAllDriversWithRealisticData() {
        final var drivers = driverService.updateAllDriversWithRealisticData();
        return ResponseEntity.ok(ApiResponse.ok(drivers));
    }

    @PostMapping("/reset-all-passwords")
    @PreAuthorize("hasRole('ADMIN')")
    @Hidden
    public ResponseEntity<ApiResponse<String>> resetAllDriverPasswords(@RequestParam String newPassword) {
        final int count = driverService.resetAllDriverPasswords(newPassword);
        return ResponseEntity.ok(ApiResponse.ok("Đã cập nhật mật khẩu cho " + count + " tài xế"));
    }
}
