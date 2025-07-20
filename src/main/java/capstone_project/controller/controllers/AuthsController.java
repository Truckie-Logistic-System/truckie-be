package capstone_project.controller.controllers;


import capstone_project.controller.dtos.request.*;
import capstone_project.controller.dtos.response.*;
import capstone_project.enums.RoleType;
import capstone_project.service.services.EmailProtocolService;
import capstone_project.service.services.RegistersService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${auth.api.base-path}")
@RequiredArgsConstructor
public class AuthsController {

    private final RegistersService registersService;
    private final EmailProtocolService emailProtocolService;

    /**
     * Register response entity.
     *
     * @param registerUserRequest the register user request
     * @return the response entity
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@RequestBody @Valid RegisterUserRequest registerUserRequest, @RequestParam RoleType roleType) {
        final var register = registersService.register(registerUserRequest, roleType);
        String otp = registersService.generateOtp();
        emailProtocolService.sendOtpEmail(registerUserRequest.getEmail(), otp);
        return ResponseEntity.ok(ApiResponse.ok(register));
    }

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
