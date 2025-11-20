package capstone_project.service.services.notification.impl;

import capstone_project.dtos.request.notification.GeneralNotificationMessageRequest;
import capstone_project.dtos.request.notification.NotificationMessageRequest;
import capstone_project.service.services.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void sendToUser(String userId, NotificationMessageRequest message) {
        
        messagingTemplate.convertAndSend("/queue/notifications/" + userId, message);
    }

    @Override
    public void sendToAll(GeneralNotificationMessageRequest message) {
        
        messagingTemplate.convertAndSend("/topic/notifications", message);
    }
}
