package capstone_project.controller.auth;


import capstone_project.dtos.request.auth.*;
import capstone_project.dtos.request.user.RegisterCustomerRequest;
import capstone_project.dtos.response.auth.ChangePasswordResponse;
import capstone_project.dtos.response.auth.LoginPublicResponse;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${auth.api.base-path}")
@RequiredArgsConstructor
public class AuthsController {

    private final RegisterService registerService;
    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
    private static final String ACCESS_TOKEN_COOKIE_NAME = "accessToken";

    /**
     * Login response entity.
     *
     * @param loginRequest the login request
     * @return the response entity
     */
    @PostMapping("")
    public ResponseEntity<ApiResponse<LoginPublicResponse>> login(
            @RequestBody @Valid LoginWithoutEmailRequest loginRequest,
            HttpServletResponse response) {
        final var login = registerService.login(loginRequest);

        // Set refresh token as a cookie
        addRefreshTokenCookie(response, login.getRefreshToken());

        // Set access token as a cookie
        addAccessTokenCookie(response, login.getAuthToken());

        var publicResp = new LoginPublicResponse(login.getUser().getId(), login.getUser().getUsername(), login.getUser().getEmail(), login.getUser().getRole().getRoleName());
        return ResponseEntity.ok(ApiResponse.ok(publicResp));
    }

    @PostMapping("/google")
    public ResponseEntity<ApiResponse<LoginResponse>> loginWithGoogle(
            @RequestBody @Valid RegisterUserRequest registerUserRequest,
            HttpServletResponse response) {
        final var login = registerService.loginWithGoogle(registerUserRequest);

        // Set refresh token as a cookie
        addRefreshTokenCookie(response, login.getRefreshToken());

        // Set access token as a cookie
        addAccessTokenCookie(response, login.getAuthToken());

        return ResponseEntity.ok(ApiResponse.ok(login));
    }

    @PostMapping("/token/refresh")
    public ResponseEntity<ApiResponse<Void>> refreshAccessToken(
            HttpServletRequest request,
            HttpServletResponse response) {
        // Extract refresh token from cookies using the service
        String refreshToken = registerService.extractRefreshTokenFromCookies(request);

        // Get the new tokens
        final var refreshTokenResponse = registerService.refreshAccessToken(refreshToken);

        // Update the refresh token cookie if it has changed
        if (!refreshToken.equals(refreshTokenResponse.getRefreshToken())) {
            addRefreshTokenCookie(response, refreshTokenResponse.getRefreshToken());
        }

        // Always set the new access token as a cookie
        addAccessTokenCookie(response, refreshTokenResponse.getAccessToken());

        // Return an empty successful response since tokens are in cookies
        return ResponseEntity.ok(ApiResponse.ok(null));
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

    /**
     * Helper method to add a refresh token cookie to the response
     */
    private void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(true); // For HTTPS
        cookie.setPath("/");
        cookie.setMaxAge(7 * 24 * 60 * 60); // 7 days, should match your token expiration
        response.addCookie(cookie);
    }

    /**
     * Helper method to add an access token cookie to the response
     */
    private void addAccessTokenCookie(HttpServletResponse response, String accessToken) {
        Cookie cookie = new Cookie(ACCESS_TOKEN_COOKIE_NAME, accessToken);
        cookie.setHttpOnly(true); // Make HTTP-only for better security
        cookie.setSecure(true); // For HTTPS
        cookie.setPath("/");
        cookie.setMaxAge(30 * 60); // 30 minutes, should match your token expiration
        response.addCookie(cookie);
    }
}