package capstone_project.service.services.email.impl;

import capstone_project.common.enums.UserStatusEnum;
import capstone_project.common.template.OtpEmailTemplate;
import capstone_project.config.expired.OtpSchedulerService;
import capstone_project.dtos.response.auth.OTPResponse;
import capstone_project.repository.entityServices.auth.UserEntityService;
import capstone_project.service.services.email.EmailProtocolService;
import capstone_project.service.services.user.CustomerService;
import capstone_project.service.services.user.UserService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import capstone_project.common.template.OtpEmailForgetPasswordTemplate;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailProtocolServiceImpl implements EmailProtocolService {

    private final UserEntityService userEntityService;
    private final UserService userService;
    private final CustomerService customerService;

    private final JavaMailSender javaMailSender;
    private final Object emailLock = new Object();
    private final OtpSchedulerService otpSchedulerService;
    private final Map<String, OTPResponse> otpStorage = new ConcurrentHashMap<>();
    
    // Separate storage for forgot password OTP and reset tokens
    private final Map<String, OTPResponse> forgotPasswordOtpStorage = new ConcurrentHashMap<>();
    private final Map<String, ResetTokenData> resetTokenStorage = new ConcurrentHashMap<>();

    @Value("${spring.mail.username}")
    private String sender;
    
    // Inner class to store reset token data
    private static class ResetTokenData {
        private final String token;
        private final LocalDateTime createdAt;
        
        public ResetTokenData(String token, LocalDateTime createdAt) {
            this.token = token;
            this.createdAt = createdAt;
        }
        
        public String getToken() { return token; }
        public LocalDateTime getCreatedAt() { return createdAt; }
    }

    @Override
    public void sendOtpEmail(String email, String otp) {
        synchronized (emailLock) {
            try {
                String emailTemplate = OtpEmailTemplate.getOtpEmailTemplate();
                String emailContent = String.format(emailTemplate, otp);

                MimeMessage message = javaMailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                helper.setTo(email);
                helper.setSubject("OTP Verification");
                helper.setText(emailContent, true);

                javaMailSender.send(message);

                otpStorage.put(email, new OTPResponse(otp, LocalDateTime.now()));

                otpSchedulerService.scheduleOtpExpirationJob(email, otp);

                introduceDelay();

            } catch (MessagingException | SchedulerException e) {
                log.error("Failed to send OTP email to {}", email, e);
                throw new RuntimeException("Failed to send OTP email", e);
            }
        }
    }

    @Override
    public boolean verifyOtp(String email, String otp) {
        OTPResponse otpData = otpStorage.get(email);

        if (otpData != null) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime createdAt = otpData.getCreatedAt();

            if (now.isBefore(createdAt.plusMinutes(5))) {
                if (otpData.getOtp().equals(otp)) {
                    otpStorage.remove(email);

                    userService.updateUserStatus(email, UserStatusEnum.ACTIVE.name());
                    customerService.updateCustomerStatus(userEntityService.getUserByEmail(email)
                            .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email))
                            .getId(), UserStatusEnum.ACTIVE.name());

                    return true;
                }
            } else {
                otpStorage.remove(email);
            }
        }

        return false;
    }

    @Override
    public void removeOtpIfExpired(String email, String otp) {
        OTPResponse otpResponse = otpStorage.get(email);
        if (otpResponse != null && otpResponse.getOtp().equals(otp)) {
            otpStorage.remove(email); // Remove the OTP
            
        }
    }

    private void introduceDelay() {
        try {
            Thread.sleep(1000); // 1 second delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Delay interrupted: {}", e.getMessage());
        }
    }

    // ==================== FORGOT PASSWORD OTP METHODS ====================

    @Override
    public void sendForgotPasswordOtp(String email) {
        // Validate user exists
        var userOpt = userEntityService.getUserByEmail(email);
        if (userOpt.isEmpty()) {
            log.warn("[sendForgotPasswordOtp] User not found with email: {}", email);
            throw new IllegalArgumentException("Không tìm thấy tài khoản với email này");
        }

        synchronized (emailLock) {
            try {
                // Generate 6-digit OTP
                String otp = generateOtp();
                String username = userOpt.get().getUsername();

                // Use forgot password email template
                String emailTemplate = OtpEmailForgetPasswordTemplate.getOtpEmailForgetPasswordTemplate();
                String emailContent = String.format(emailTemplate, username, otp);

                MimeMessage message = javaMailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                helper.setTo(email);
                helper.setSubject("Yêu cầu đặt lại mật khẩu - Truckie");
                helper.setText(emailContent, true);

                javaMailSender.send(message);

                // Store OTP with timestamp (valid for 5 minutes)
                forgotPasswordOtpStorage.put(email, new OTPResponse(otp, LocalDateTime.now()));

                log.info("[sendForgotPasswordOtp] OTP sent successfully to: {}", email);

                introduceDelay();

            } catch (MessagingException e) {
                log.error("[sendForgotPasswordOtp] Failed to send OTP email to {}", email, e);
                throw new RuntimeException("Không thể gửi email OTP. Vui lòng thử lại sau.", e);
            }
        }
    }

    @Override
    public String verifyForgotPasswordOtp(String email, String otp) {
        OTPResponse otpData = forgotPasswordOtpStorage.get(email);

        if (otpData == null) {
            log.warn("[verifyForgotPasswordOtp] No OTP found for email: {}", email);
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime createdAt = otpData.getCreatedAt();

        // OTP valid for 5 minutes
        if (now.isAfter(createdAt.plusMinutes(5))) {
            forgotPasswordOtpStorage.remove(email);
            log.warn("[verifyForgotPasswordOtp] OTP expired for email: {}", email);
            return null;
        }

        if (!otpData.getOtp().equals(otp)) {
            log.warn("[verifyForgotPasswordOtp] Invalid OTP for email: {}", email);
            return null;
        }

        // OTP is valid - remove it and generate reset token
        forgotPasswordOtpStorage.remove(email);

        // Generate reset token (valid for 10 minutes)
        String resetToken = UUID.randomUUID().toString();
        resetTokenStorage.put(email, new ResetTokenData(resetToken, LocalDateTime.now()));

        log.info("[verifyForgotPasswordOtp] OTP verified successfully for email: {}", email);
        return resetToken;
    }

    @Override
    public boolean validateResetToken(String email, String resetToken) {
        ResetTokenData tokenData = resetTokenStorage.get(email);

        if (tokenData == null) {
            log.warn("[validateResetToken] No reset token found for email: {}", email);
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime createdAt = tokenData.getCreatedAt();

        // Reset token valid for 10 minutes
        if (now.isAfter(createdAt.plusMinutes(10))) {
            resetTokenStorage.remove(email);
            log.warn("[validateResetToken] Reset token expired for email: {}", email);
            return false;
        }

        if (!tokenData.getToken().equals(resetToken)) {
            log.warn("[validateResetToken] Invalid reset token for email: {}", email);
            return false;
        }

        return true;
    }

    @Override
    public void invalidateResetToken(String email) {
        resetTokenStorage.remove(email);
        log.info("[invalidateResetToken] Reset token invalidated for email: {}", email);
    }

    private String generateOtp() {
        return String.format("%06d", new Random().nextInt(999999));
    }

}
