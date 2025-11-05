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
        log.info("📢 Broadcasting new issue: {} - {}", issue.id(), issue.description());
        
        try {
            messagingTemplate.convertAndSend("/topic/issues/new", issue);
            log.info("✅ Issue broadcast completed");
        } catch (Exception e) {
            log.error("❌ Error broadcasting new issue: {}", e.getMessage(), e);
        }
    }

    /**
     * Broadcast issue status change to all staff clients
     * @param issue The updated issue
     */
    public void broadcastIssueStatusChange(GetBasicIssueResponse issue) {
        log.info("📢 Broadcasting issue status change: {} - status: {}", 
                 issue.id(), issue.status());
        
        try {
            messagingTemplate.convertAndSend("/topic/issues/status-change", issue);
            log.info("✅ Issue status change broadcast completed");
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
        log.info("📲 Sending seal assignment notification to driver: {}", driverId);
        
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
            
            log.info("✅ Seal assignment notification sent to driver: {}", driverId);
        } catch (Exception e) {
            log.error("❌ Error sending seal assignment notification: {}", e.getMessage(), e);
        }
    }
}
