package capstone_project.service.services.websocket;

import capstone_project.dtos.response.issue.GetBasicIssueResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Service for broadcasting Issue-related WebSocket messages
 * Separates WebSocket logic from business logic
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IssueWebSocketService {
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Broadcast new issue to all staff clients
     * @param issue The newly created issue
     */
    public void broadcastNewIssue(GetBasicIssueResponse issue) {

        try {
            messagingTemplate.convertAndSend("/topic/issues/new", issue);
            
        } catch (Exception e) {
            log.error("❌ Error broadcasting new issue: {}", e.getMessage(), e);
        }
    }

    /**
     * Broadcast issue status change to all staff clients
     * @param issue The updated issue
     */
    public void broadcastIssueStatusChange(GetBasicIssueResponse issue) {

        try {
            messagingTemplate.convertAndSend("/topic/issues/status-change", issue);
            
        } catch (Exception e) {
            log.error("❌ Error broadcasting issue status change: {}", e.getMessage(), e);
        }
    }

    /**
     * Send seal assignment notification to specific driver
     * @param driverId Driver user ID
     * @param issue The updated issue with new seal
     * @param staffName Staff who assigned the seal
     * @param newSealCode New seal code
     * @param oldSealCode Old seal code
     */
    public void sendSealAssignmentNotification(
            String driverId, 
            GetBasicIssueResponse issue,
            String staffName,
            String newSealCode,
            String oldSealCode) {

        try {
            // Create notification payload
            var notification = new java.util.HashMap<String, Object>();
            notification.put("type", "SEAL_ASSIGNMENT");
            notification.put("priority", "URGENT");
            notification.put("title", "Seal mới đã được gán");
            notification.put("message", String.format(
                "Nhân viên %s đã gán seal mới %s để thay thế seal cũ %s. Vui lòng gắn seal mới và chụp ảnh xác nhận.",
                staffName, newSealCode, oldSealCode
            ));
            notification.put("issue", issue);
            notification.put("timestamp", java.time.Instant.now().toString());
            
            // Send to specific driver via user-specific topic
            messagingTemplate.convertAndSend(
                "/topic/driver/" + driverId + "/notifications", 
                notification
            );

        } catch (Exception e) {
            log.error("❌ Error sending seal assignment notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Send notification to specific driver for ORDER_REJECTION payment success
     * @param driverId Driver user ID
     * @param issueId Issue ID
     * @param vehicleAssignmentId Vehicle assignment ID
     * @param returnJourneyId Return journey ID
     * @param orderId Order ID
     */
    public void sendReturnPaymentSuccessNotification(
            java.util.UUID driverId,
            java.util.UUID issueId,
            java.util.UUID vehicleAssignmentId,
            java.util.UUID returnJourneyId,
            java.util.UUID orderId) {

        try {
            // Create notification payload
            var notification = new java.util.HashMap<String, Object>();
            notification.put("type", "RETURN_PAYMENT_SUCCESS");
            notification.put("priority", "HIGH");
            notification.put("title", "Khách hàng đã thanh toán");
            notification.put("message", "Khách hàng đã thanh toán cước trả hàng. Vui lòng tiến hành trả hàng về điểm pickup.");
            notification.put("issueId", issueId.toString());
            notification.put("vehicleAssignmentId", vehicleAssignmentId.toString());
            if (returnJourneyId != null) {
                notification.put("returnJourneyId", returnJourneyId.toString());
            }
            if (orderId != null) {
                notification.put("orderId", orderId.toString());
            }
            notification.put("timestamp", java.time.Instant.now().toString());
            
            // Send to specific driver via user-specific topic
            messagingTemplate.convertAndSend(
                "/topic/driver/" + driverId + "/notifications", 
                notification
            );

        } catch (Exception e) {
            log.error("❌ Error sending return payment notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Send notification to specific driver for ORDER_REJECTION payment timeout
     * When customer doesn't pay within deadline, driver should continue original route
     * @param driverId Driver user ID
     * @param issueId Issue ID
     * @param vehicleAssignmentId Vehicle assignment ID
     */
    public void sendReturnPaymentTimeoutNotification(
            java.util.UUID driverId,
            java.util.UUID issueId,
            java.util.UUID vehicleAssignmentId) {

        try {
            // Create notification payload
            var notification = new java.util.HashMap<String, Object>();
            notification.put("type", "RETURN_PAYMENT_TIMEOUT");
            notification.put("priority", "HIGH");
            notification.put("title", "Khách hàng không thanh toán");
            notification.put("message", "Khách hàng đã hết hạn thanh toán cước trả hàng. Vui lòng tiếp tục theo lộ trình ban đầu về carrier. Các kiện hàng bị từ chối sẽ được hủy.");
            notification.put("issueId", issueId.toString());
            notification.put("vehicleAssignmentId", vehicleAssignmentId.toString());
            notification.put("timestamp", java.time.Instant.now().toString());
            
            // Send to specific driver via user-specific topic
            messagingTemplate.convertAndSend(
                "/topic/driver/" + driverId + "/notifications", 
                notification
            );

        } catch (Exception e) {
            log.error("❌ Error sending return payment timeout notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Send notification to specific driver when damage issue is resolved
     * Driver can continue the trip after staff resolves the issue
     * @param driverId Driver user ID
     * @param issue The resolved issue
     * @param staffName Staff who resolved the issue
     */
    public void sendDamageResolvedNotification(
            String driverId,
            GetBasicIssueResponse issue,
            String staffName) {

        try {
            // Create notification payload
            var notification = new java.util.HashMap<String, Object>();
            notification.put("type", "DAMAGE_RESOLVED");
            notification.put("priority", "HIGH");
            notification.put("title", "Sự cố hàng hóa đã được xử lý");
            notification.put("message", String.format(
                "Nhân viên %s đã xử lý xong sự cố hàng hóa bị hư hỏng. Bạn có thể tiếp tục hành trình vận chuyển.",
                staffName
            ));
            notification.put("issue", issue);
            notification.put("timestamp", java.time.Instant.now().toString());
            
            // Send to specific driver via user-specific topic
            messagingTemplate.convertAndSend(
                "/topic/driver/" + driverId + "/notifications", 
                notification
            );

        } catch (Exception e) {
            log.error("❌ Error sending damage resolved notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Send return payment success notification to all staff
     * Broadcasts to all staff users via public topic
     * @param issueId Issue ID
     * @param orderId Order ID
     * @param customerName Customer name
     * @param returnJourneyId Return journey ID
     * @param trackingCodes Tracking codes của các kiện hàng bị reject
     * @param paymentAmount Số tiền thanh toán
     * @param vehicleAssignmentCode Mã chuyến xe
     */
    public void sendReturnPaymentSuccessNotificationToStaff(
            java.util.UUID issueId,
            java.util.UUID orderId,
            String customerName,
            java.util.UUID returnJourneyId,
            String trackingCodes,
            java.math.BigDecimal paymentAmount,
            String vehicleAssignmentCode) {

        try {
            // Create notification payload
            var notification = new java.util.HashMap<String, Object>();
            notification.put("type", "RETURN_PAYMENT_SUCCESS_STAFF");
            notification.put("priority", "HIGH");
            notification.put("title", "Khách hàng đã thanh toán cước trả hàng");
            notification.put("message", String.format(
                "<b>Khách hàng:</b> %s\n" +
                "<b>Kiện hàng:</b> %s\n" +
                "<b>Chuyến:</b> %s\n" +
                "<b>Số tiền:</b> %,d VND",
                customerName != null ? customerName : "N/A",
                trackingCodes != null ? trackingCodes : "N/A",
                vehicleAssignmentCode != null ? vehicleAssignmentCode : "N/A",
                paymentAmount != null ? paymentAmount.longValue() : 0
            ));
            notification.put("issueId", issueId.toString());
            notification.put("orderId", orderId.toString());
            notification.put("trackingCodes", trackingCodes);
            notification.put("paymentAmount", paymentAmount);
            notification.put("vehicleAssignmentCode", vehicleAssignmentCode);
            if (returnJourneyId != null) {
                notification.put("returnJourneyId", returnJourneyId.toString());
            }
            notification.put("timestamp", java.time.Instant.now().toString());
            
            // Broadcast to all staff via public topic
            messagingTemplate.convertAndSend(
                "/topic/issues/return-payment-success", 
                notification
            );

        } catch (Exception e) {
            log.error("❌ Error broadcasting return payment notification to staff: {}", e.getMessage(), e);
        }
    }

    /**
     * Send return payment timeout notification to all staff
     * Broadcasts to all staff users via public topic
     * @param issueId Issue ID
     * @param orderId Order ID
     * @param customerName Customer name
     * @param trackingCodes Tracking codes của các kiện hàng bị reject
     * @param vehicleAssignmentCode Mã chuyến xe
     */
    public void sendReturnPaymentTimeoutNotificationToStaff(
            java.util.UUID issueId,
            java.util.UUID orderId,
            String customerName,
            String trackingCodes,
            String vehicleAssignmentCode) {

        try {
            // Create notification payload
            var notification = new java.util.HashMap<String, Object>();
            notification.put("type", "RETURN_PAYMENT_TIMEOUT_STAFF");
            notification.put("priority", "MEDIUM");
            notification.put("title", "Khách hàng không thanh toán cước trả hàng");
            notification.put("message", String.format(
                "<b>Khách hàng:</b> %s\n" +
                "<b>Kiện hàng:</b> %s\n" +
                "<b>Chuyến:</b> %s\n" +
                "<b>Trạng thái:</b> Quá hạn thanh toán - Đã hủy",
                customerName != null ? customerName : "N/A",
                trackingCodes != null ? trackingCodes : "N/A",
                vehicleAssignmentCode != null ? vehicleAssignmentCode : "N/A"
            ));
            notification.put("issueId", issueId.toString());
            if (orderId != null) {
                notification.put("orderId", orderId.toString());
            }
            notification.put("trackingCodes", trackingCodes);
            notification.put("vehicleAssignmentCode", vehicleAssignmentCode);
            notification.put("timestamp", java.time.Instant.now().toString());
            
            // Broadcast to all staff via public topic
            messagingTemplate.convertAndSend(
                "/topic/issues/return-payment-timeout", 
                notification
            );

        } catch (Exception e) {
            log.error("❌ Error broadcasting return payment timeout notification to staff: {}", e.getMessage(), e);
        }
    }

    /**
     * Send return payment timeout notification to customer
     * Notifies customer that payment deadline has expired and packages are cancelled
     * @param customerId Customer user ID
     * @param issueId Issue ID
     * @param orderId Order ID
     * @param trackingCodes Tracking codes của các kiện hàng bị hủy
     */
    public void sendReturnPaymentTimeoutNotificationToCustomer(
            java.util.UUID customerId,
            java.util.UUID issueId,
            java.util.UUID orderId,
            String trackingCodes) {

        try {
            // Create notification payload
            var notification = new java.util.HashMap<String, Object>();
            notification.put("type", "RETURN_PAYMENT_TIMEOUT");
            notification.put("priority", "HIGH");
            notification.put("title", "Hết hạn thanh toán cước trả hàng");
            notification.put("message", String.format(
                "Đơn hàng của bạn đã hết hạn thanh toán cước trả hàng.\n" +
                "Các kiện hàng bị từ chối (%s) đã được hủy.\n" +
                "Vui lòng kiểm tra lại đơn hàng.",
                trackingCodes != null ? trackingCodes : "N/A"
            ));
            notification.put("issueId", issueId.toString());
            notification.put("orderId", orderId.toString());
            notification.put("trackingCodes", trackingCodes);
            notification.put("timestamp", java.time.Instant.now().toString());
            
            // Send to specific customer via user-specific topic
            messagingTemplate.convertAndSend(
                "/topic/user/" + customerId + "/notifications", 
                notification
            );

        } catch (Exception e) {
            log.error("❌ Error sending return payment timeout notification to customer: {}", e.getMessage(), e);
        }
    }

    /**
     * Send message to staff when driver confirms new seal attachment
     * @param staffId Staff user ID who assigned the seal
     * @param driverName Driver who confirmed the seal
     * @param newSealCode New seal code that was attached
     * @param oldSealCode Old seal code that was removed
     * @param sealImageUrl URL of the seal attachment image
     * @param oldSealImageUrl URL of the old seal removal image
     * @param trackingCode Tracking code
     * @param journeyCode Journey code for display
     */
    public void sendSealConfirmationMessageToStaff(
            String staffId,
            String driverName,
            String newSealCode,
            String oldSealCode,
            String sealImageUrl,
            String oldSealImageUrl,
            String trackingCode,
            String journeyCode) {

        try {
            // Create message payload for staff
            var message = new java.util.HashMap<String, Object>();
            message.put("type", "SEAL_CONFIRMATION");
            message.put("priority", "MEDIUM");
            message.put("title", "Driver đã xác nhận gắn seal mới");
            message.put("message", String.format(
                "Driver %s đã xác nhận gắn seal mới %s thành công.",
                driverName, newSealCode
            ));
            message.put("driverName", driverName);
            message.put("newSealCode", newSealCode);
            message.put("oldSealCode", oldSealCode);
            message.put("sealImageUrl", sealImageUrl);
            message.put("oldSealImage", oldSealImageUrl);
            message.put("trackingCode", trackingCode);
            message.put("journeyCode", journeyCode);
            message.put("timestamp", java.time.Instant.now().toString());
            
            // Send to specific staff via user-specific topic
            messagingTemplate.convertAndSend(
                "/topic/staff/" + staffId + "/messages", 
                message
            );

        } catch (Exception e) {
            log.error("❌ Error sending seal confirmation message to staff: {}", e.getMessage(), e);
        }
    }

    /**
     * Send notification to specific driver when reroute issue is resolved
     * Staff has created new journey, driver should fetch and continue with new route
     * @param driverId Driver ID (NOT user ID)
     * @param issueId Issue ID
     * @param orderId Order ID
     */
    public void sendRerouteResolvedNotification(
            java.util.UUID driverId,
            java.util.UUID issueId,
            java.util.UUID orderId) {

        try {
            // Create notification payload
            var notification = new java.util.HashMap<String, Object>();
            notification.put("type", "REROUTE_RESOLVED");
            notification.put("priority", "HIGH");
            notification.put("title", "Lộ trình mới đã sẵn sàng");
            notification.put("message", "Nhân viên đã tạo lộ trình mới để tránh khu vực gặp sự cố. Vui lòng kiểm tra và tiếp tục theo lộ trình mới.");
            notification.put("issueId", issueId.toString());
            notification.put("orderId", orderId.toString());
            notification.put("timestamp", java.time.Instant.now().toString());
            
            // CRITICAL: Use driver ID (not user ID) to match mobile app subscription
            // Mobile subscribes to /topic/driver/{DRIVER_ID}/notifications
            messagingTemplate.convertAndSend(
                "/topic/driver/" + driverId + "/notifications", 
                notification
            );
            
            log.info("✅ Sent reroute resolved notification to driver: {} (issue: {})", 
                    driverId, issueId);

        } catch (Exception e) {
            log.error("❌ Error sending reroute resolved notification: {}", e.getMessage(), e);
        }
    }
}
