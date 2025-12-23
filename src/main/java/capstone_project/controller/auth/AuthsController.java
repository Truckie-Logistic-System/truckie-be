package capstone_project.controller.auth;

import capstone_project.dtos.request.auth.*;
import capstone_project.dtos.request.user.RegisterCustomerRequest;
import capstone_project.dtos.response.auth.CustomerRegisterResponse;
import capstone_project.dtos.response.auth.AccessTokenResponse;
import capstone_project.dtos.response.auth.ChangePasswordResponse;
import capstone_project.dtos.response.auth.LoginResponse;
import capstone_project.dtos.response.auth.RefreshTokenResponse;
import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.dtos.response.user.CustomerResponse;
import capstone_project.service.services.auth.RegisterService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${auth.api.base-path}")
@Slf4j
@RequiredArgsConstructor
public class AuthsController {

    private final RegisterService registerService;

    @PostMapping("")
    public ResponseEntity<ApiResponse<AccessTokenResponse>> login(
            @RequestBody @Valid LoginWithoutEmailRequest loginRequest,
            HttpServletResponse response) {
        final var login = registerService.login(loginRequest);

        // set refresh token as HttpOnly cookie
        registerService.addRefreshTokenCookie(response, login.getRefreshToken());

        var accessDto = AccessTokenResponse.builder()
                .authToken(login.getAuthToken())
                .user(login.getUser())
                .firstTimeLogin(login.getFirstTimeLogin())
                .requiredActions(login.getRequiredActions())
                .build();

        return ResponseEntity.ok(ApiResponse.ok(accessDto));
    }

    @PostMapping("/google")
    public ResponseEntity<ApiResponse<LoginResponse>> loginWithGoogle(
            @RequestBody @Valid RegisterUserRequest registerUserRequest,
            HttpServletResponse response) {
        final var login = registerService.loginWithGoogle(registerUserRequest);

        // Set refresh token as a cookie
        // addRefreshTokenCookie(response, login.getRefreshToken());

        // Set access token as a cookie
        // addAccessTokenCookie(response, login.getAuthToken());

        return ResponseEntity.ok(ApiResponse.ok(login));
    }

    @PostMapping("/token/refresh")
    public ResponseEntity<ApiResponse<AccessTokenResponse>> refreshAccessToken(
            HttpServletRequest request,
            HttpServletResponse response) {
        
        String refreshToken = registerService.extractRefreshTokenFromCookies(request);

        final var refreshTokenResponse = registerService.refreshAccessToken(refreshToken);

        // IMPORTANT: Set new refresh token cookie after token rotation
        // This ensures the client always has the latest valid refresh token
        registerService.addRefreshTokenCookie(response, refreshTokenResponse.getRefreshToken());

        // Return ONLY access token in body
        var accessTokenResponse = AccessTokenResponse.builder()
                .authToken(refreshTokenResponse.getAccessToken())
                .build();

        return ResponseEntity.ok(ApiResponse.ok(accessTokenResponse));
    }

    @PostMapping("/customer/register")
    public ResponseEntity<ApiResponse<CustomerRegisterResponse>> register(@RequestBody @Valid RegisterCustomerRequest registerCustomerRequest) {
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
    public ResponseEntity<ApiResponse<Boolean>> logout(HttpServletRequest request, HttpServletResponse response) {
        boolean success = registerService.logout(request, response);
        return ResponseEntity.ok(ApiResponse.ok(success));
    }

    @PostMapping("/mobile/logout")
    public ResponseEntity<ApiResponse<Boolean>> logoutMobile(@RequestBody RefreshTokenRequest refreshTokenRequest) {
        boolean success = registerService.logout(refreshTokenRequest.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.ok(success));
    }

    @PostMapping("/mobile")
    public ResponseEntity<ApiResponse<LoginResponse>> loginMobile(
            @RequestBody @Valid LoginWithoutEmailRequest loginRequest) {
        final var login = registerService.login(loginRequest);
        return ResponseEntity.ok(ApiResponse.ok(login));
    }

    @PostMapping("/mobile/token/refresh")
    public ResponseEntity<ApiResponse<RefreshTokenResponse>> refreshAccessTokenMobile(
            @RequestBody @Valid RefreshTokenRequest refreshTokenRequest) {
        org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(this.getClass());
        
        
        final var refreshTokenResponse = registerService.refreshAccessToken(refreshTokenRequest.getRefreshToken());

        return ResponseEntity.ok(ApiResponse.ok(refreshTokenResponse));
    }

    @GetMapping("/check-username")
    public ResponseEntity<ApiResponse<Boolean>> checkUsernameAvailability(@RequestParam String username) {
        boolean isAvailable = registerService.isUsernameAvailable(username);
        return ResponseEntity.ok(ApiResponse.ok(isAvailable));
    }

    @GetMapping("/check-email")
    public ResponseEntity<ApiResponse<Boolean>> checkEmailAvailability(@RequestParam String email) {
        boolean isAvailable = registerService.isEmailAvailable(email);
        return ResponseEntity.ok(ApiResponse.ok(isAvailable));
    }
}