package capstone_project.controller.controllers;


import capstone_project.controller.dtos.request.LoginWithoutEmailRequest;
import capstone_project.controller.dtos.request.RefreshTokenRequest;
import capstone_project.controller.dtos.request.RegisterUserRequest;
import capstone_project.controller.dtos.response.ApiResponse;
import capstone_project.controller.dtos.response.LoginResponse;
import capstone_project.controller.dtos.response.RefreshTokenResponse;
import capstone_project.controller.dtos.response.UserResponse;
import capstone_project.enums.RoleType;
import capstone_project.service.services.EmailProtocolService;
import capstone_project.service.services.RegistersService;
import lombok.RequiredArgsConstructor;
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
    public ApiResponse<UserResponse> register(@RequestBody RegisterUserRequest registerUserRequest, @RequestParam RoleType roleType) {
        final var register = registersService.register(registerUserRequest, roleType);
        String otp = registersService.generateOtp();
        emailProtocolService.sendOtpEmail(registerUserRequest.getEmail(), otp);
        return ApiResponse.ok(register);
    }

    /**
     * Login response entity.
     *
     * @param loginRequest the login request
     * @return the response entity
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginWithoutEmailRequest loginRequest) {
        final var login = registersService.login(loginRequest);
        return ApiResponse.ok(login);
    }

    @PostMapping("/login/google")
    public ApiResponse<LoginResponse> loginWithGoogle(@RequestBody RegisterUserRequest registerUserRequest) {
        final var login = registersService.loginWithGoogle(registerUserRequest);
        return ApiResponse.ok(login);
    }

    @PostMapping("/refresh/access-token")
    public ApiResponse<RefreshTokenResponse> refreshAccessToken(@RequestBody RefreshTokenRequest refreshTokenRequest) {
        final var refreshTokenResponse = registersService.refreshAccessToken(refreshTokenRequest);
        return ApiResponse.ok(refreshTokenResponse);
    }

}
