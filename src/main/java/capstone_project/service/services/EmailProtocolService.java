package capstone_project.service.services;

public interface EmailProtocolService {

    void sendOtpEmail(String email, String otpCode);

    boolean verifyOtp(String email, String otpCode);

    void removeOtpIfExpired(String email, String otp);

}
