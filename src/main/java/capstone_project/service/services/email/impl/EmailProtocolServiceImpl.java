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
import java.util.concurrent.ConcurrentHashMap;

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

    @Value("${spring.mail.username}")
    private String sender;

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

}
