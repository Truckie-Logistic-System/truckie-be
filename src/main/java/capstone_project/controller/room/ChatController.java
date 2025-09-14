package capstone_project.controller.room;

import capstone_project.dtos.request.room.MessageRequest;
import capstone_project.dtos.response.room.ChatPageResponse;
import capstone_project.dtos.response.room.ChatResponseDTO;
import capstone_project.service.services.room.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

//@RestController
//@RequestMapping("${chat.api.base-path}")
@Controller
@RequiredArgsConstructor
public class ChatController {
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;

    @MessageMapping("/chat.sendMessage/{roomId}")
    public void handleChatMessage(
            @DestinationVariable String roomId,
             MessageRequest messageRequest
    ) {
        System.out.println("Received message: " + messageRequest);
        chatService.saveMessage(messageRequest)
                .thenAccept(saved -> {
                    // Gửi message này đến tất cả client đang subscribe
                    messagingTemplate.convertAndSend("/topic/room/" + roomId, saved);
                });
    }

    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<ChatPageResponse> getMessages(
            @PathVariable String roomId,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String lastMessageId
    ) throws ExecutionException, InterruptedException {
        ChatPageResponse response = chatService.getMessagesByRoomId(roomId, pageSize, lastMessageId);
        return ResponseEntity.ok(response);
    }

}
