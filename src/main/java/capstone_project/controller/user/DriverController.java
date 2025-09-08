package capstone_project.controller.user;

import capstone_project.dtos.request.user.UpdateDriverRequest;
import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.dtos.response.user.DriverResponse;
import capstone_project.service.services.user.DriverService;
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

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<DriverResponse>> getDriverByUserId(@PathVariable UUID userId) {
        final var driver = driverService.getDriverByUserId(userId);
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

}
