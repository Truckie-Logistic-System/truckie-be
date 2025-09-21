package capstone_project.controller.auth;


import capstone_project.common.utils.CookieUtil;
import capstone_project.dtos.request.auth.*;
import capstone_project.dtos.request.user.RegisterCustomerRequest;
import capstone_project.dtos.response.auth.ChangePasswordResponse;
import capstone_project.dtos.response.auth.LoginResponse;
import capstone_project.dtos.response.auth.RefreshTokenResponse;
import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.dtos.response.user.CustomerResponse;
import capstone_project.service.services.auth.RegisterService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${auth.api.base-path}")
@RequiredArgsConstructor
public class AuthsController {

    private final RegisterService registerService;
    private final CookieUtil cookieUtil;

    /**
     * Login response entity.
     *
     * @param loginRequest the login request
     * @return the response entity
     */
    @PostMapping("")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @RequestBody @Valid LoginWithoutEmailRequest loginRequest,
            HttpServletResponse response) {
        final var login = registerService.login(loginRequest);

        // Set HTTP-only cookies for both access and refresh tokens
        cookieUtil.createAccessTokenCookie(response, login.getAuthToken());
        cookieUtil.createRefreshTokenCookie(response, login.getRefreshToken());

        return ResponseEntity.ok(ApiResponse.ok(login));
    }

    @PostMapping("/google")
    public ResponseEntity<ApiResponse<LoginResponse>> loginWithGoogle(
            @RequestBody @Valid RegisterUserRequest registerUserRequest,
            HttpServletResponse response) {
        final var login = registerService.loginWithGoogle(registerUserRequest);

        // Set HTTP-only cookies for both access and refresh tokens
        cookieUtil.createAccessTokenCookie(response, login.getAuthToken());
        cookieUtil.createRefreshTokenCookie(response, login.getRefreshToken());

        return ResponseEntity.ok(ApiResponse.ok(login));
    }

    @PostMapping("/token/refresh")
    public ResponseEntity<ApiResponse<RefreshTokenResponse>> refreshAccessToken(
            @RequestBody @Valid RefreshTokenRequest refreshTokenRequest,
            HttpServletResponse response) {
        final var refreshTokenResponse = registerService.refreshAccessToken(refreshTokenRequest);

        // Set HTTP-only cookie for the new access token
        cookieUtil.createAccessTokenCookie(response, refreshTokenResponse.getAccessToken());

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

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(HttpServletResponse response) {
        // Clear all auth cookies
        cookieUtil.clearAuthCookies(response);
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully"));
    }
}
