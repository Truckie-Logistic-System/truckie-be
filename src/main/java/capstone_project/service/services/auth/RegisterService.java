package capstone_project.service.services.auth;

import capstone_project.common.enums.RoleTypeEnum;
import capstone_project.dtos.request.auth.*;
import capstone_project.dtos.request.user.RegisterCustomerRequest;
import capstone_project.dtos.response.auth.CustomerRegisterResponse;
import capstone_project.dtos.request.user.RegisterDriverRequest;
import capstone_project.dtos.response.auth.ChangePasswordResponse;
import capstone_project.dtos.response.auth.LoginResponse;
import capstone_project.dtos.response.auth.RefreshTokenResponse;
import capstone_project.dtos.response.auth.UserResponse;
import capstone_project.dtos.response.user.CustomerResponse;
import capstone_project.dtos.response.user.DriverResponse;
import capstone_project.dtos.response.demo.DemoUsersGenerationResponse;
import capstone_project.dtos.response.demo.UpdateUsernamesResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * The interface Registers service.
 */
public interface RegisterService {
    /**
     * Register user response.
     *
     * @param registerUserRequest the register user request
     * @return the user response
     */
    UserResponse register(RegisterUserRequest registerUserRequest, RoleTypeEnum roleTypeEnum);

    CustomerRegisterResponse registerCustomer(RegisterCustomerRequest registerCustomerRequest);

    DriverResponse registerDriver(RegisterDriverRequest registerDriverRequest);

    /**
     * Login login response.
     *
     * @param loginRequest the login request
     * @return the login response
     */
    LoginResponse login(LoginWithoutEmailRequest loginRequest);

    LoginResponse loginWithGoogle(RegisterUserRequest registerUserRequest);

    /**
     * Refresh access token using the refresh token from the request body
     *
     * @param refreshTokenRequest the refresh token request
     * @return the refresh token response
     * @deprecated Use {@link #refreshAccessToken(String)} instead
     */
    @Deprecated
    RefreshTokenResponse refreshAccessToken(RefreshTokenRequest refreshTokenRequest);

    /**
     * Refresh access token using the refresh token string
     *
     * @param refreshToken the refresh token string
     * @return the refresh token response
     */
    RefreshTokenResponse refreshAccessToken(String refreshToken);

    /**
     * Extract refresh token from the request cookies
     *
     * @param request the HTTP request containing cookies
     * @return the extracted refresh token
     */
    String extractRefreshTokenFromCookies(HttpServletRequest request);

    void addRefreshTokenCookie(HttpServletResponse response, String refreshToken);

    String generateOtp();

    ChangePasswordResponse changePassword(ChangePasswordRequest changePasswordRequest);

    ChangePasswordResponse changePasswordForForgetPassword(ChangePasswordForForgetPassRequest changePasswordForForgetPassRequest);

    /**
     * Logout the user by invalidating their refresh token
     *
     * @param request the HTTP request containing the refresh token cookie
     * @param response the HTTP response to clear the refresh token cookie
     * @return true if logout was successful, false otherwise
     */
    boolean logout(HttpServletRequest request, HttpServletResponse response);

    /**
     * Logout the user using the provided refresh token
     *
     * @param refreshToken the refresh token to invalidate
     * @return true if logout was successful, false otherwise
     */
    boolean logout(String refreshToken);

    /**
     * Generate demo users (drivers, customers, staff) for dashboard demo
     * CreatedAt dates are distributed throughout December 2025 with focus on Dec 22-27
     *
     * @return Summary of generated users
     */
    DemoUsersGenerationResponse generateDemoUsers();

    /**
     * Update all existing usernames to correct format:
     * - Customer: firstname + lastname_abbreviation (e.g., datn for Nguyễn Văn Đạt)
     * - Driver: driver + firstname + lastname_abbreviation (e.g., driverdatn)
     * - Staff: staff + firstname + lastname_abbreviation (e.g., staffdatn)
     * 
     * @return Summary of updated usernames
     */
    UpdateUsernamesResponse updateAllUsernamesToCorrectFormat();
}
