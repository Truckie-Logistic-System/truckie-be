package capstone_project.controller.controllers;


import capstone_project.controller.dtos.request.LoginWithoutEmailRequest;
import capstone_project.controller.dtos.request.RefreshTokenRequest;
import capstone_project.controller.dtos.request.RegisterCustomerRequest;
import capstone_project.controller.dtos.request.RegisterUserRequest;
import capstone_project.controller.dtos.response.ApiResponse;
import capstone_project.controller.dtos.response.CustomerResponse;
import capstone_project.controller.dtos.response.LoginResponse;
import capstone_project.controller.dtos.response.RefreshTokenResponse;
import capstone_project.service.services.EmailProtocolService;
import capstone_project.service.services.RegistersService;
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

    private final RegistersService registersService;
    private final EmailProtocolService emailProtocolService;

    @PostMapping("/register/customer")
    public ResponseEntity<ApiResponse<CustomerResponse>> register(@RequestBody @Valid RegisterCustomerRequest registerCustomerRequest) {
        final var register = registersService.registerCustomer(registerCustomerRequest);
        String otp = registersService.generateOtp();
        emailProtocolService.sendOtpEmail(registerCustomerRequest.getEmail(), otp);
        return ResponseEntity.ok(ApiResponse.ok(register));
    }

    /**
     * Login response entity.
     *
     * @param loginRequest the login request
     * @return the response entity
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody @Valid LoginWithoutEmailRequest loginRequest) {
        final var login = registersService.login(loginRequest);
        return ResponseEntity.ok(ApiResponse.ok(login));
    }

    @PostMapping("/login/google")
    public ResponseEntity<ApiResponse<LoginResponse>> loginWithGoogle(@RequestBody @Valid RegisterUserRequest registerUserRequest) {
        final var login = registersService.loginWithGoogle(registerUserRequest);
        return ResponseEntity.ok(ApiResponse.ok(login));
    }

    @PostMapping("/refresh/access-token")
    public ResponseEntity<ApiResponse<RefreshTokenResponse>> refreshAccessToken(@RequestBody @Valid RefreshTokenRequest refreshTokenRequest) {
        final var refreshTokenResponse = registersService.refreshAccessToken(refreshTokenRequest);
        return ResponseEntity.ok(ApiResponse.ok(refreshTokenResponse));
    }

}
