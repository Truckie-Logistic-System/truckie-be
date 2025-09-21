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

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("${chat.api.base-path}")
@Controller
@RequiredArgsConstructor
public class ChatController {
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;


    //Đây là api để send message realtime và lưu vào firebase
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

    //Đây là lấy tất cả messages từ roomID
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<ChatPageResponse> getMessages(
            @PathVariable String roomId,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String lastMessageId
    ) throws ExecutionException, InterruptedException {
        ChatPageResponse response = chatService.getMessagesByRoomId(roomId, pageSize, lastMessageId);
        return ResponseEntity.ok(response);
    }


    //Đây là api lấy tất cả messegas dành cho customer mà customer đó nhắn trên system để tư vấn (những loại tin nhắn này thuộc về RoomType
    // SUPPORT(là chỉ customer nhắn nhưng chưa được reply vì staff chưa được assign vào room chat đó)
    // và SUPPORTED(là room chat đã được assign staff vào))
    // , chỉ dành cho customer
    @GetMapping("/{userId}/messages-supported")
    public ResponseEntity<ChatPageResponse> getMessagesSupportedForCustomer(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String lastMessageId
    ) throws ExecutionException, InterruptedException {
        ChatPageResponse response = chatService.getMessagesForRoomSupportForCusByUserId(userId, pageSize, lastMessageId);
        return ResponseEntity.ok(response);
    }

}
