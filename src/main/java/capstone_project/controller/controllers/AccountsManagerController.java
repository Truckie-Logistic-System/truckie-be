package capstone_project.controller.controllers;

import capstone_project.controller.dtos.request.RegisterDriverRequest;
import capstone_project.controller.dtos.request.RegisterUserRequest;
import capstone_project.controller.dtos.response.ApiResponse;
import capstone_project.controller.dtos.response.DriverResponse;
import capstone_project.controller.dtos.response.UserResponse;
import capstone_project.enums.RoleType;
import capstone_project.service.services.RegistersService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${manager.api.base-path}")
@RequiredArgsConstructor
public class AccountsManagerController {

    private final RegistersService registersService;

    /**
     * Register response entity.
     *
     * @param registerUserRequest the register user request
     * @return the response entity
     */
    @PostMapping("/register")
//    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> register(@RequestBody @Valid RegisterUserRequest registerUserRequest, @RequestParam RoleType roleType) {
        final var register = registersService.register(registerUserRequest, roleType);
        return ResponseEntity.ok(ApiResponse.ok(register));
    }

    @PostMapping("/register/driver")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<DriverResponse>> registerDriver(@RequestBody @Valid RegisterDriverRequest registerDriverRequest) {
        final var register = registersService.registerDriver(registerDriverRequest);
        return ResponseEntity.ok(ApiResponse.ok(register));
    }
}
