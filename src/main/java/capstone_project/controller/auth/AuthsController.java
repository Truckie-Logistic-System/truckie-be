package capstone_project.controller.auth;


import capstone_project.dtos.request.auth.LoginWithoutEmailRequest;
import capstone_project.dtos.request.auth.RefreshTokenRequest;
import capstone_project.dtos.request.auth.RegisterUserRequest;
import capstone_project.dtos.request.user.RegisterCustomerRequest;
import capstone_project.dtos.response.auth.LoginResponse;
import capstone_project.dtos.response.auth.RefreshTokenResponse;
import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.dtos.response.user.CustomerResponse;
import capstone_project.service.services.auth.RegisterService;
import capstone_project.service.services.email.EmailProtocolService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${auth.api.base-path}")
@RequiredArgsConstructor
public class AuthsController {

    private final RegisterService registerService;
    private final EmailProtocolService emailProtocolService;

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
        String otp = registerService.generateOtp();
        emailProtocolService.sendOtpEmail(registerCustomerRequest.getEmail(), otp);
        return ResponseEntity.ok(ApiResponse.ok(register));
    }
}
