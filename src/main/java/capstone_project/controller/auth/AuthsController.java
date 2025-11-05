package capstone_project.controller.auth;


import capstone_project.dtos.request.auth.*;
import capstone_project.dtos.request.user.RegisterCustomerRequest;
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
        log.info("[refreshAccessToken] START - Refresh token endpoint called");
        String refreshToken = registerService.extractRefreshTokenFromCookies(request);
        log.info("[refreshAccessToken] Extracted refresh token from cookies: {}...", refreshToken.substring(0, Math.min(20, refreshToken.length())));

        final var refreshTokenResponse = registerService.refreshAccessToken(refreshToken);
        log.info("[refreshAccessToken] Got new access token from service");

        // NO NEED to set refresh token cookie again since we're keeping the same token
        // This prevents cookie sync issues on page reload
        // The existing refresh token cookie is still valid and will continue to work
        log.info("[refreshAccessToken] Keeping existing refresh token cookie (no rotation)");

        // Return ONLY access token in body
        var accessTokenResponse = AccessTokenResponse.builder()
                .authToken(refreshTokenResponse.getAccessToken())
                .build();
        
        log.info("[refreshAccessToken] ✅ Returning access token response");
        return ResponseEntity.ok(ApiResponse.ok(accessTokenResponse));
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
        log.info("[refreshAccessTokenMobile] START - Received mobile token refresh request");
        log.info("[refreshAccessTokenMobile] Refresh token: {}...", refreshTokenRequest.getRefreshToken().substring(0, Math.min(20, refreshTokenRequest.getRefreshToken().length())));
        
        final var refreshTokenResponse = registerService.refreshAccessToken(refreshTokenRequest.getRefreshToken());
        
        log.info("[refreshAccessTokenMobile] ✅ Returning new tokens with user info");
        log.info("[refreshAccessTokenMobile] New access token: {}...", refreshTokenResponse.getAccessToken().substring(0, 20));
        log.info("[refreshAccessTokenMobile] New refresh token: {}...", refreshTokenResponse.getRefreshToken().substring(0, 20));
        log.info("[refreshAccessTokenMobile] User info included: username={}, role={}", 
                refreshTokenResponse.getUser() != null ? refreshTokenResponse.getUser().getUsername() : "null",
                refreshTokenResponse.getUser() != null && refreshTokenResponse.getUser().getRole() != null ? refreshTokenResponse.getUser().getRole().getRoleName() : "null");
        
        return ResponseEntity.ok(ApiResponse.ok(refreshTokenResponse));
    }
}