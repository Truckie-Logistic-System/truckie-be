package capstone_project.controller.admin;

import capstone_project.dtos.request.user.RegisterDriverRequest;
import capstone_project.dtos.request.auth.RegisterUserRequest;
import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.dtos.response.user.DriverResponse;
import capstone_project.dtos.response.auth.UserResponse;
import capstone_project.common.enums.RoleTypeEnum;
import capstone_project.service.services.auth.RegisterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${manager.api.base-path}")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class AccountsManagerController {

    private final RegisterService registerService;

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

    @PostMapping("/driver/register")
    public ResponseEntity<ApiResponse<DriverResponse>> registerDriver(@RequestBody @Valid RegisterDriverRequest registerDriverRequest) {
        final var register = registerService.registerDriver(registerDriverRequest);
        return ResponseEntity.ok(ApiResponse.ok(register));
    }
}
