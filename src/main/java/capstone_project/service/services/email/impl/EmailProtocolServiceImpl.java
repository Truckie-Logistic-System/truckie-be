package capstone_project.service.services.email.impl;

import org.springframework.scheduling.annotation.Async;

import capstone_project.common.enums.UserStatusEnum;
import capstone_project.common.template.OtpEmailTemplate;
import capstone_project.config.expired.OtpSchedulerService;
import capstone_project.dtos.response.auth.OTPResponse;
import capstone_project.entity.auth.UserEntity;
import capstone_project.repository.entityServices.auth.UserEntityService;
import capstone_project.repository.repositories.user.DriverRepository;
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
    private final DriverRepository driverRepository;

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
    @Async
    public void sendOtpEmail(String email, String otp) {
        try {
            log.info("[📧 sendOtpEmail] Sending OTP email to: {}", email);
            
            String emailTemplate = OtpEmailTemplate.getOtpEmailTemplate();
            // Sử dụng replace thay vì String.format để tránh lỗi với các ký tự đặc biệt trong HTML
            String emailContent = emailTemplate.replace("%s", otp);

            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(email);
            helper.setSubject("Xác thực OTP - Truckie");
            helper.setText(emailContent, true);
            helper.setFrom(sender);

            javaMailSender.send(message);
            
            // Lưu OTP vào storage
            otpStorage.put(email, new OTPResponse(otp, LocalDateTime.now()));
            
            // Lên lịch hết hạn OTP
            try {
                otpSchedulerService.scheduleOtpExpirationJob(email, otp);
            } catch (SchedulerException se) {
                log.warn("[📧 sendOtpEmail] Failed to schedule OTP expiration: {}", se.getMessage());
                // Không throw exception ở đây, vì OTP vẫn được gửi thành công
            }

            log.info("[📧 sendOtpEmail] OTP sent successfully to: {}", email);
            
        } catch (Exception e) {
            log.error("[❌ sendOtpEmail] Failed to send OTP email to {}: {}", email, e.getMessage(), e);
            throw new RuntimeException("Không thể gửi email OTP. Vui lòng thử lại sau.", e);
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

                    // Cập nhật trạng thái của UserEntity thành ACTIVE (đã xác thực OTP)
                    userService.updateUserStatus(email, UserStatusEnum.ACTIVE.name());
                    
                    // Cập nhật trạng thái của CustomerEntity thành INACTIVE (đã xác thực OTP nhưng chờ admin kích hoạt)
                    customerService.updateCustomerStatus(userEntityService.getUserByEmail(email)
                            .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email))
                            .getId(), UserStatusEnum.INACTIVE.name());
                    
                    log.info("[verifyOtp] OTP verified successfully for email: {}. User is now ACTIVE but Customer is INACTIVE waiting for admin approval", email);
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

    // Phương thức introduceDelay() đã được loại bỏ vì các phương thức gửi email đã được đánh dấu @Async

    // ==================== FORGOT PASSWORD OTP METHODS ====================

    @Override
    @Async
    public void sendForgotPasswordOtp(String email) {
        // Validate user exists - check both users table directly and via driver relationship
        log.info("[📧 sendForgotPasswordOtp] Looking for user with email: {}", email);
        var userOpt = userEntityService.getUserByEmail(email);
        UserEntity user = null;
        
        if (userOpt.isPresent()) {
            user = userOpt.get();
            log.info("[📧 sendForgotPasswordOtp] Found user directly: {}", user.getUsername());
        } else {
            log.info("[📧 sendForgotPasswordOtp] User not found directly, trying via driver relationship...");
            // Try to find user via driver relationship
            var driverOpt = driverRepository.findByUserEmail(email);
            if (driverOpt.isPresent()) {
                user = driverOpt.get().getUser();
                log.info("[📧 sendForgotPasswordOtp] Found user via driver: {}", user.getUsername());
            } else {
                log.warn("[📧 sendForgotPasswordOtp] Driver not found with user email: {}", email);
            }
        }
        
        if (user == null) {
            log.warn("[📧 sendForgotPasswordOtp] User not found with email: {}", email);
            throw new IllegalArgumentException("Không tìm thấy tài khoản với email này");
        }

        try {
            log.info("[📧 sendForgotPasswordOtp] Sending password reset OTP to: {}", email);
            
            // Generate 6-digit OTP
            String otp = generateOtp();
            String username = user.getUsername();

            // Use forgot password email template
            String emailTemplate = OtpEmailForgetPasswordTemplate.getOtpEmailForgetPasswordTemplate();
            // Replace username first, then OTP (use replaceFirst to replace only first occurrence)
            String emailContent = emailTemplate.replaceFirst("%s", username);
            emailContent = emailContent.replaceFirst("%s", otp);

            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(email);
            helper.setSubject("Yêu cầu đặt lại mật khẩu - Truckie");
            helper.setText(emailContent, true);
            helper.setFrom(sender);

            javaMailSender.send(message);

            // Store OTP with timestamp (valid for 5 minutes)
            forgotPasswordOtpStorage.put(email, new OTPResponse(otp, LocalDateTime.now()));

            log.info("[📧 sendForgotPasswordOtp] OTP sent successfully to: {}", email);

        } catch (Exception e) {
            log.error("[❌ sendForgotPasswordOtp] Failed to send OTP email to {}: {}", email, e.getMessage(), e);
            throw new RuntimeException("Không thể gửi email OTP. Vui lòng thử lại sau.", e);
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
