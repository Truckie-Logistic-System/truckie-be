package capstone_project.service.services.email.impl;

import capstone_project.entity.NotificationEntity;
import capstone_project.entity.auth.UserEntity;
import capstone_project.service.services.email.EmailNotificationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.internet.MimeMessage;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of email notification service using Spring Mail and Thymeleaf templates
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationServiceImpl implements EmailNotificationService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final ObjectMapper objectMapper;

    @Value("${spring.mail.from:noreply@truckie.vn}")
    private String fromEmail;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Override
    @Async
    @Transactional
    public void sendNotificationEmail(NotificationEntity notification, UserEntity user) {
        try {
            log.info("📧 Sending email notification to customer: {}", user.getEmail());
            
            // Parse metadata JSON string to Map for template access
            Map<String, Object> metadataMap = new HashMap<>();
            if (notification.getMetadata() != null && !notification.getMetadata().isEmpty()) {
                try {
                    metadataMap = objectMapper.readValue(notification.getMetadata(), new TypeReference<Map<String, Object>>() {});
                } catch (Exception e) {
                    log.warn("⚠️ Failed to parse notification metadata: {}", e.getMessage());
                }
            }
            
            Context context = new Context();
            context.setVariable("notification", notification);
            context.setVariable("metadata", metadataMap); // Pass parsed metadata as Map
            context.setVariable("user", user);
            context.setVariable("frontendUrl", frontendUrl);
            context.setVariable("vietnameseType", notification.getNotificationType().getVietnameseLabel());
            
            String emailContent = templateEngine.process("emails/notification-template", context);
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(user.getEmail());
            helper.setSubject(notification.getTitle());
            helper.setText(emailContent, true);
            
            mailSender.send(message);
            
            log.info("✅ Email sent successfully to: {}", user.getEmail());
            
        } catch (Exception e) {
            log.error("❌ Failed to send email to {}: {}", user.getEmail(), e.getMessage(), e);
            // Don't throw exception to avoid breaking main notification flow
        }
    }

    @Override
    @Async
    @Transactional
    public void sendSealReplacementEmail(String customerEmail, String orderCode, 
                                         String oldSealCode, String newSealCode, 
                                         String reason, String staffName) {
        try {
            log.info("📧 Sending seal replacement email to: {}", customerEmail);
            
            Context context = new Context();
            context.setVariable("orderCode", orderCode);
            context.setVariable("oldSealCode", oldSealCode);
            context.setVariable("newSealCode", newSealCode);
            context.setVariable("reason", reason);
            context.setVariable("staffName", staffName);
            context.setVariable("frontendUrl", frontendUrl);
            
            String emailContent = templateEngine.process("emails/seal-replacement", context);
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(customerEmail);
            helper.setSubject("🔐 Thông báo thay thế seal - Đơn hàng " + orderCode);
            helper.setText(emailContent, true);
            
            mailSender.send(message);
            
            log.info("✅ Seal replacement email sent successfully to: {}", customerEmail);
            
        } catch (Exception e) {
            log.error("❌ Failed to send seal replacement email to {}: {}", customerEmail, e.getMessage(), e);
        }
    }

    @Override
    @Async
    @Transactional
    public void sendPaymentSuccessEmail(String customerEmail, String orderCode, 
                                       String amount, String paymentMethod) {
        try {
            log.info("📧 Sending payment success email to: {}", customerEmail);
            
            Context context = new Context();
            context.setVariable("orderCode", orderCode);
            context.setVariable("amount", amount);
            context.setVariable("paymentMethod", paymentMethod);
            context.setVariable("frontendUrl", frontendUrl);
            
            String emailContent = templateEngine.process("emails/payment-success", context);
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(customerEmail);
            helper.setSubject("💰 Xác nhận thanh toán thành công - Đơn hàng " + orderCode);
            helper.setText(emailContent, true);
            
            mailSender.send(message);
            
            log.info("✅ Payment success email sent successfully to: {}", customerEmail);
            
        } catch (Exception e) {
            log.error("❌ Failed to send payment success email to {}: {}", customerEmail, e.getMessage(), e);
        }
    }

    @Override
    @Async
    @Transactional
    public void sendOrderStatusChangeEmail(String customerEmail, String orderCode, 
                                          String oldStatus, String newStatus, 
                                          String description) {
        try {
            log.info("📧 Sending order status change email to: {}", customerEmail);
            
            Context context = new Context();
            context.setVariable("orderCode", orderCode);
            context.setVariable("oldStatus", oldStatus);
            context.setVariable("newStatus", newStatus);
            context.setVariable("description", description);
            context.setVariable("frontendUrl", frontendUrl);
            
            String emailContent = templateEngine.process("emails/order-status-change", context);
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(customerEmail);
            helper.setSubject("📦 Cập nhật trạng thái đơn hàng " + orderCode);
            helper.setText(emailContent, true);
            
            mailSender.send(message);
            
            log.info("✅ Order status change email sent successfully to: {}", customerEmail);
            
        } catch (Exception e) {
            log.error("❌ Failed to send order status change email to {}: {}", customerEmail, e.getMessage(), e);
        }
    }
    
    @Override
    @Async
    @Transactional
    public void sendDriverAssignmentEmail(String driverEmail, String driverName, 
                                         String orderCode, Map<String, Object> metadata) {
        try {
            log.info("📧 Sending driver assignment email to: {}", driverEmail);
            
            Context context = new Context();
            context.setVariable("driverName", driverName);
            context.setVariable("orderCode", orderCode);
            context.setVariable("metadata", metadata);
            context.setVariable("frontendUrl", frontendUrl);
            
            String emailContent = templateEngine.process("emails/driver-assignment", context);
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(driverEmail);
            helper.setSubject("🚛 Thông báo phân công chuyến xe - Đơn hàng " + orderCode);
            helper.setText(emailContent, true);
            
            mailSender.send(message);
            
            log.info("✅ Driver assignment email sent successfully to: {}", driverEmail);
            
        } catch (Exception e) {
            log.error("❌ Failed to send driver assignment email to {}: {}", driverEmail, e.getMessage(), e);
        }
    }
    
    @Override
    @Async
    @Transactional
    public void sendStaffCredentialsEmail(String staffEmail, String staffName, 
                                         String username, String tempPassword) {
        try {
            log.info("📧 Sending staff credentials email to: {}", staffEmail);
            
            Context context = new Context();
            context.setVariable("staffName", staffName);
            context.setVariable("username", username);
            context.setVariable("tempPassword", tempPassword);
            context.setVariable("frontendUrl", frontendUrl);
            
            String emailContent = templateEngine.process("emails/staff-credentials", context);
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(staffEmail);
            helper.setSubject("👤 Thông tin đăng nhập tài khoản nhân viên Truckie");
            helper.setText(emailContent, true);
            
            mailSender.send(message);
            
            log.info("✅ Staff credentials email sent successfully to: {}", staffEmail);
            
        } catch (Exception e) {
            log.error("❌ Failed to send staff credentials email to {}: {}", staffEmail, e.getMessage(), e);
        }
    }
}
