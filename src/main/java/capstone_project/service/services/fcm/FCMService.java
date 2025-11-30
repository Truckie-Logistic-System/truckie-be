package capstone_project.service.services.fcm;

import com.google.firebase.messaging.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class FCMService {

    private final FirebaseMessaging firebaseMessaging;

    public FCMService(FirebaseMessaging firebaseMessaging) {
        this.firebaseMessaging = firebaseMessaging;
    }

    /**
     * Send notification to a single device
     */
    @Async
    public void sendNotificationToToken(String token, String title, String body, Map<String, String> data) {
        try {
            // Build notification
            Notification notification = Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build();

            // Build message
            Message message = Message.builder()
                .setToken(token)
                .setNotification(notification)
                .putAllData(data)
                .setAndroidConfig(AndroidConfig.builder()
                    .setPriority(AndroidConfig.Priority.HIGH)
                    .setNotification(AndroidNotification.builder()
                        .setColor("#2196F3") // Blue color for Truckie
                        .setIcon("@drawable/ic_notification")
                        .setSound("default")
                        .build())
                    .build())
                .setApnsConfig(ApnsConfig.builder()
                    .setAps(Aps.builder()
                        .setSound("default")
                        .setBadge(1)
                        .build())
                    .build())
                .build();

            // Send message
            String messageId = firebaseMessaging.send(message);
            log.info("Successfully sent FCM message to token: {} with ID: {}", token, messageId);

        } catch (FirebaseMessagingException e) {
            log.error("Failed to send FCM message to token: {}", token, e);
            
            // Handle specific error cases
            if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                log.warn("FCM token is unregistered, should be removed from database: {}", token);
            }
        }
    }

    /**
     * Send notification to multiple devices
     */
    @Async
    public void sendNotificationToTokens(List<String> tokens, String title, String body, Map<String, String> data) {
        if (tokens == null || tokens.isEmpty()) {
            log.warn("No FCM tokens provided for multicast message");
            return;
        }

        try {
            // Build notification
            Notification notification = Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build();

            // Build multicast message
            MulticastMessage message = MulticastMessage.builder()
                .addAllTokens(tokens)
                .setNotification(notification)
                .putAllData(data)
                .setAndroidConfig(AndroidConfig.builder()
                    .setPriority(AndroidConfig.Priority.HIGH)
                    .setNotification(AndroidNotification.builder()
                        .setColor("#2196F3")
                        .setIcon("@drawable/ic_notification")
                        .setSound("default")
                        .build())
                    .build())
                .setApnsConfig(ApnsConfig.builder()
                    .setAps(Aps.builder()
                        .setSound("default")
                        .setBadge(1)
                        .build())
                    .build())
                .build();

            // Send multicast message using modern API
            BatchResponse response = firebaseMessaging.sendEachForMulticast(message);
            log.info("Successfully sent FCM multicast message. Success: {}, Failure: {}", 
                    response.getSuccessCount(), response.getFailureCount());

            // Handle failed tokens
            if (response.getFailureCount() > 0) {
                List<SendResponse> responses = response.getResponses();
                for (int i = 0; i < responses.size(); i++) {
                    if (!responses.get(i).isSuccessful()) {
                        String failedToken = tokens.get(i);
                        log.warn("Failed to send to token: {} - Reason: {}", 
                                failedToken, responses.get(i).getException().getMessage());
                    }
                }
            }

        } catch (FirebaseMessagingException e) {
            log.error("Failed to send FCM multicast message", e);
        }
    }

    /**
     * Send data-only notification (silent push)
     */
    @Async
    public void sendDataNotification(String token, Map<String, String> data) {
        try {
            Message message = Message.builder()
                .setToken(token)
                .putAllData(data)
                .build();

            String messageId = firebaseMessaging.send(message);
            log.info("Successfully sent data FCM message with ID: {}", messageId);

        } catch (FirebaseMessagingException e) {
            log.error("Failed to send data FCM message", e);
        }
    }

    /**
     * Subscribe tokens to a topic
     */
    @Async
    public void subscribeToTopic(List<String> tokens, String topic) {
        try {
            TopicManagementResponse response = firebaseMessaging.subscribeToTopic(tokens, topic);
            log.info("Successfully subscribed {} tokens to topic: {}", 
                    response.getSuccessCount(), topic);
        } catch (FirebaseMessagingException e) {
            log.error("Failed to subscribe tokens to topic: {}", topic, e);
        }
    }

    /**
     * Unsubscribe tokens from a topic
     */
    @Async
    public void unsubscribeFromTopic(List<String> tokens, String topic) {
        try {
            TopicManagementResponse response = firebaseMessaging.unsubscribeFromTopic(tokens, topic);
            log.info("Successfully unsubscribed {} tokens from topic: {}", 
                    response.getSuccessCount(), topic);
        } catch (FirebaseMessagingException e) {
            log.error("Failed to unsubscribe tokens from topic: {}", topic, e);
        }
    }
}
