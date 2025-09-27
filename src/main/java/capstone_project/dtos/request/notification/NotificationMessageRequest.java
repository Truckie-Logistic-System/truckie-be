package capstone_project.dtos.request.notification;

import java.time.LocalDateTime;

public record NotificationMessageRequest(
        String content,
        String type,
        LocalDateTime timeSend
) {
}
