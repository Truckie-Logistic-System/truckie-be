package capstone_project.dtos.request.notification;

import java.time.LocalDateTime;

public record GeneralNotificationMessageRequest(
//        String userId,
        String content,
        String type,
        LocalDateTime timeSend
) {
}
