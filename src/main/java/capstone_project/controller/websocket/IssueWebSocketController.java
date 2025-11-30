package capstone_project.controller.websocket;

import capstone_project.dtos.response.issue.GetBasicIssueResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;

/**
 * WebSocket Controller for real-time Issue notifications
 * Broadcasts new issues to all connected staff clients
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class IssueWebSocketController {
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Mobile app gọi sau khi tạo issue thành công để broadcast đến staff
     * Mobile sends to: /app/issue/created
     * Server broadcasts to: /topic/issues/new (all staff receive)
     */
    @MessageMapping("/issue/created")
    public void broadcastNewIssue(@Payload GetBasicIssueResponse issue) {

        // Broadcast to all staff clients listening to /topic/issues/new
        messagingTemplate.convertAndSend("/topic/issues/new", issue);

    }

    /**
     * Broadcast issue status change (OPEN -> IN_PROGRESS -> RESOLVED)
     * Server broadcasts to: /topic/issues/status-change
     */
    public void broadcastIssueStatusChange(GetBasicIssueResponse issue) {

        messagingTemplate.convertAndSend("/topic/issues/status-change", issue);

    }
}
