package capstone_project.service.services.email;

public interface EmailProtocolService {

    void sendOtpEmail(String email, String otpCode);

    boolean verifyOtp(String email, String otpCode);

    void removeOtpIfExpired(String email, String otp);

    // Forgot Password OTP methods
    void sendForgotPasswordOtp(String email);

    /**
     * Verify OTP for forgot password flow.
     * Returns a reset token if OTP is valid, null otherwise.
     */
    String verifyForgotPasswordOtp(String email, String otp);

    /**
     * Validate reset token for password reset.
     */
    boolean validateResetToken(String email, String resetToken);

    /**
     * Invalidate reset token after password reset.
     */
    void invalidateResetToken(String email);

}
