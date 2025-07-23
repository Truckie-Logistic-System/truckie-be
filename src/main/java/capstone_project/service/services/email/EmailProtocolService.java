package capstone_project.service.services.email;

public interface EmailProtocolService {

    void sendOtpEmail(String email, String otpCode);

    boolean verifyOtp(String email, String otpCode);

    void removeOtpIfExpired(String email, String otp);

}
