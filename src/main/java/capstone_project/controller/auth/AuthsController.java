package capstone_project.controller.auth;


import capstone_project.dtos.request.auth.*;
import capstone_project.dtos.request.user.RegisterCustomerRequest;
import capstone_project.dtos.response.auth.ChangePasswordResponse;
import capstone_project.dtos.response.auth.LoginResponse;
import capstone_project.dtos.response.auth.RefreshTokenResponse;
import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.dtos.response.user.CustomerResponse;
import capstone_project.service.services.auth.RegisterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${auth.api.base-path}")
@RequiredArgsConstructor
public class AuthsController {

    private final RegisterService registerService;

    /**
     * Login response entity.
     *
     * @param loginRequest the login request
     * @return the response entity
     */
    @PostMapping("")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody @Valid LoginWithoutEmailRequest loginRequest) {
        final var login = registerService.login(loginRequest);
        return ResponseEntity.ok(ApiResponse.ok(login));
    }

    @PostMapping("/google")
    public ResponseEntity<ApiResponse<LoginResponse>> loginWithGoogle(@RequestBody @Valid RegisterUserRequest registerUserRequest) {
        final var login = registerService.loginWithGoogle(registerUserRequest);
        return ResponseEntity.ok(ApiResponse.ok(login));
    }

    @PostMapping("/token/refresh")
    public ResponseEntity<ApiResponse<RefreshTokenResponse>> refreshAccessToken(@RequestBody @Valid RefreshTokenRequest refreshTokenRequest) {
        final var refreshTokenResponse = registerService.refreshAccessToken(refreshTokenRequest);
        return ResponseEntity.ok(ApiResponse.ok(refreshTokenResponse));
    }

    @PostMapping("/customer/register")
    public ResponseEntity<ApiResponse<CustomerResponse>> register(@RequestBody @Valid RegisterCustomerRequest registerCustomerRequest) {
        final var register = registerService.registerCustomer(registerCustomerRequest);
        return ResponseEntity.ok(ApiResponse.ok(register));
    }

    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<ChangePasswordResponse>> changePassword(@RequestBody final ChangePasswordRequest changePasswordRequest) {
        final var changePasswordResponse = registerService.changePassword(changePasswordRequest);
        return ResponseEntity.ok(ApiResponse.ok(changePasswordResponse));
    }

    @PutMapping("/change-password-for-forget-password")
    public ResponseEntity<ApiResponse<ChangePasswordResponse>> changePasswordForForgetPassword(@RequestBody final ChangePasswordForForgetPassRequest changePasswordForForgetPassRequest) {
        final var changePasswordResponse = registerService.changePasswordForForgetPassword(changePasswordForForgetPassRequest);
        return ResponseEntity.ok(ApiResponse.ok(changePasswordResponse));
    }

}
