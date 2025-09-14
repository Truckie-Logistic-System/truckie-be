package capstone_project.service.services.auth;

import capstone_project.common.enums.RoleTypeEnum;
import capstone_project.dtos.request.auth.*;
import capstone_project.dtos.request.user.RegisterCustomerRequest;
import capstone_project.dtos.request.user.RegisterDriverRequest;
import capstone_project.dtos.response.auth.ChangePasswordResponse;
import capstone_project.dtos.response.auth.LoginResponse;
import capstone_project.dtos.response.auth.RefreshTokenResponse;
import capstone_project.dtos.response.auth.UserResponse;
import capstone_project.dtos.response.user.CustomerResponse;
import capstone_project.dtos.response.user.DriverResponse;

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

    CustomerResponse registerCustomer(RegisterCustomerRequest registerCustomerRequest);

    DriverResponse registerDriver(RegisterDriverRequest registerDriverRequest);

    /**
     * Login login response.
     *
     * @param loginRequest the login request
     * @return the login response
     */
    LoginResponse login(LoginWithoutEmailRequest loginRequest);

    LoginResponse loginWithGoogle(RegisterUserRequest registerUserRequest);

    RefreshTokenResponse refreshAccessToken(RefreshTokenRequest refreshTokenRequest);

    String generateOtp();

    ChangePasswordResponse changePassword(ChangePasswordRequest changePasswordRequest);

    ChangePasswordResponse changePasswordForForgetPassword(ChangePasswordForForgetPassRequest changePasswordForForgetPassRequest);

}
