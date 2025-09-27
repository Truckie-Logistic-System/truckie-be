package capstone_project.service.services.notification;

import capstone_project.dtos.request.notification.GeneralNotificationMessageRequest;
import capstone_project.dtos.request.notification.NotificationMessageRequest;

public interface NotificationService {
    void sendToUser(String userId, NotificationMessageRequest message);

    void sendToAll(GeneralNotificationMessageRequest message);
}
