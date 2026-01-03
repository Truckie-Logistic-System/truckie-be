package capstone_project.controller.admin;

import capstone_project.dtos.request.user.AdminCreateDriverRequest;
import capstone_project.dtos.request.user.RegisterDriverRequest;
import capstone_project.dtos.request.auth.RegisterUserRequest;
import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.dtos.response.user.DriverCreatedResponse;
import capstone_project.dtos.response.user.DriverResponse;
import capstone_project.dtos.response.user.DuplicateUserCleanupResponse;
import capstone_project.dtos.response.auth.UserResponse;
import capstone_project.common.enums.RoleTypeEnum;
import capstone_project.service.services.auth.RegisterService;
import capstone_project.service.services.driver.DriverOnboardingService;
import capstone_project.service.services.user.UserCleanupService;
import io.swagger.v3.oas.annotations.Hidden;
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
    private final UserCleanupService userCleanupService;

    /**
     * Register response entity.
     * This method handles the registration of staff and admin only.
     * @param registerUserRequest the register user request
     * @return the response entity
     */
    /**
     * Register employee with optional password
     * For staff registration, password is optional and will be generated automatically
     * Login credentials will be sent to the staff's email
     */
    @PostMapping("/employee/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@RequestBody @Valid RegisterUserRequest registerUserRequest, @RequestParam RoleTypeEnum roleTypeEnum) {
        // For staff registration, password is generated automatically
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

    /**
     * Cleanup duplicate users - find and remove users with the same username.
     * For each duplicate group, keeps the oldest (first created) user and deletes the rest.
     * Also deletes associated customer and driver records.
     * 
     * @param dryRun if true, only report what would be deleted without actually deleting (default: true for safety)
     * @return Response containing details about deleted users
     */
    @DeleteMapping("/users/cleanup-duplicates")
    @Hidden
    public ResponseEntity<ApiResponse<DuplicateUserCleanupResponse>> cleanupDuplicateUsers(
            @RequestParam(defaultValue = "true") boolean dryRun) {
        log.info("[cleanupDuplicateUsers] Starting duplicate user cleanup. DryRun: {}", dryRun);
        
        DuplicateUserCleanupResponse response = userCleanupService.cleanupDuplicateUsers(dryRun);
        
        log.info("[cleanupDuplicateUsers] Completed. Found {} duplicate groups, {} users {}", 
                response.getTotalDuplicateGroupsFound(), 
                response.getTotalUsersDeleted(),
                dryRun ? "would be deleted" : "deleted");
        
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
