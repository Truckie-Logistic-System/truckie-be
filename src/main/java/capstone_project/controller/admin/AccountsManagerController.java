package capstone_project.controller.admin;

import capstone_project.dtos.request.user.AdminCreateDriverRequest;
import capstone_project.dtos.request.user.RegisterDriverRequest;
import capstone_project.dtos.request.auth.RegisterUserRequest;
import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.dtos.response.user.DriverCreatedResponse;
import capstone_project.dtos.response.user.DriverResponse;
import capstone_project.dtos.response.auth.UserResponse;
import capstone_project.common.enums.RoleTypeEnum;
import capstone_project.service.services.auth.RegisterService;
import capstone_project.service.services.driver.DriverOnboardingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${manager.api.base-path}")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAuthority('ADMIN')")
public class AccountsManagerController {

    private final RegisterService registerService;
    private final DriverOnboardingService driverOnboardingService;

    /**
     * Register response entity.
     * This method handles the registration of staff and admin only.
     * @param registerUserRequest the register user request
     * @return the response entity
     */
    @PostMapping("/employee/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@RequestBody @Valid RegisterUserRequest registerUserRequest, @RequestParam RoleTypeEnum roleTypeEnum) {
        final var register = registerService.register(registerUserRequest, roleTypeEnum);
        return ResponseEntity.ok(ApiResponse.ok(register));
    }

    /**
     * Create a new driver account (Admin only).
     * Generates random temporary password and sets driver status to INACTIVE.
     * Driver must complete onboarding on first login.
     * 
     * @param request Driver creation request (no password required)
     * @return Created driver info with temporary password
     */
    @PostMapping("/driver/create")
    public ResponseEntity<ApiResponse<DriverCreatedResponse>> createDriver(
            @RequestBody @Valid AdminCreateDriverRequest request) {
        log.info("[createDriver] Admin creating new driver: {}", request.getUsername());
        
        DriverCreatedResponse response = driverOnboardingService.createDriverByAdmin(request);
        
        log.info("[createDriver] Driver created successfully. Username: {}, TempPassword provided to admin", 
                request.getUsername());
        
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * @deprecated Use {@link #createDriver(AdminCreateDriverRequest)} instead.
     * This endpoint is kept for backward compatibility but will be removed in future versions.
     */
    @Deprecated
    @PostMapping("/driver/register")
    public ResponseEntity<ApiResponse<DriverResponse>> registerDriver(@RequestBody @Valid RegisterDriverRequest registerDriverRequest) {
        final var register = registerService.registerDriver(registerDriverRequest);
        return ResponseEntity.ok(ApiResponse.ok(register));
    }
}
