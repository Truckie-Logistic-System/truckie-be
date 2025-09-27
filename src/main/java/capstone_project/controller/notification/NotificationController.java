package capstone_project.controller.notification;

import capstone_project.dtos.request.notification.GeneralNotificationMessageRequest;
import capstone_project.dtos.request.notification.NotificationMessageRequest;
import capstone_project.service.services.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${notification.api.base-path}")
@RequiredArgsConstructor
//@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/broadcast")
    public ResponseEntity<Void> broadcast(@RequestBody GeneralNotificationMessageRequest message) {
        notificationService.sendToAll(message);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/user/{userId}")
    public ResponseEntity<Void> sendToUser(@PathVariable String userId,
                                           @RequestBody NotificationMessageRequest message) {
        notificationService.sendToUser(userId, message);
        return ResponseEntity.ok().build();
    }
}
