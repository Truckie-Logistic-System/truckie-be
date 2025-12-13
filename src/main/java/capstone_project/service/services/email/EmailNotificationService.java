package capstone_project.service.services.email;

import capstone_project.entity.NotificationEntity;
import capstone_project.entity.auth.UserEntity;

import java.util.Map;

/**
 * Service for sending email notifications to customers
 */
public interface EmailNotificationService {
    
    /**
     * Send notification email to customer
     * @param notification The notification entity containing email content
     * @param user The user to send email to
     */
    void sendNotificationEmail(NotificationEntity notification, UserEntity user);
    
    /**
     * Send seal replacement notification email
     * @param customerEmail Customer email address
     * @param orderCode Order code
     * @param oldSealCode Old seal code that was removed
     * @param newSealCode New seal code that was attached
     * @param reason Reason for seal replacement
     * @param staffName Staff member who performed the replacement
     */
    void sendSealReplacementEmail(String customerEmail, String orderCode, 
                                  String oldSealCode, String newSealCode, 
                                  String reason, String staffName);
    
    /**
     * Send payment success notification email
     * @param customerEmail Customer email address
     * @param orderCode Order code
     * @param amount Payment amount
     * @param paymentMethod Payment method used
     */
    void sendPaymentSuccessEmail(String customerEmail, String orderCode, 
                                String amount, String paymentMethod);
    
    /**
     * Send order status change notification email
     * @param customerEmail Customer email address
     * @param orderCode Order code
     * @param oldStatus Previous order status
     * @param newStatus New order status
     * @param description Additional description
     */
    void sendOrderStatusChangeEmail(String customerEmail, String orderCode, 
                                   String oldStatus, String newStatus, 
                                   String description);
    
    /**
     * Send driver assignment notification email
     * @param driverEmail Driver email address
     * @param driverName Driver name
     * @param orderCode Order code
     * @param metadata Assignment metadata including package details
     */
    void sendDriverAssignmentEmail(String driverEmail, String driverName, 
                                  String orderCode, Map<String, Object> metadata);
}
