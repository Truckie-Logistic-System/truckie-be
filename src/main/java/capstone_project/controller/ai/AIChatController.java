package capstone_project.controller.ai;

import capstone_project.dtos.request.chat.ChatMessageRequest;
import capstone_project.dtos.response.chat.ChatMessageResponse;
import capstone_project.service.services.ai.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/chat")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AI Chatbot", description = "AI chatbot hỗ trợ khách hàng")
public class AIChatController {

    private final ChatService chatService;

    @PostMapping("/message")
    @Operation(summary = "Gửi message cho AI chatbot", description = "Public endpoint - không cần authentication")
    public ResponseEntity<ChatMessageResponse> sendMessage(@Valid @RequestBody ChatMessageRequest request) {
        log.info("🤖 [Chat Request] message='{}', session={}", 
                request.message().substring(0, Math.min(50, request.message().length())), 
                request.sessionId());

        ChatMessageResponse response = chatService.processMessage(request);

        log.info("✅ [Chat Response] session={}, hasPrice={}", 
                response.getSessionId(), 
                response.getPriceEstimate() != null);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    @Operation(summary = "Health check cho chatbot service")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("AI Chatbot is running");
    }

    @PostMapping("/personality")
    @Operation(summary = "Đặt personality cho AI (PROFESSIONAL, FRIENDLY, EXPERT, QUICK)")
    public ResponseEntity<String> setPersonality(
            @RequestParam String userId,
            @RequestParam String personality) {
        chatService.setPersonality(userId, personality);
        return ResponseEntity.ok("Personality updated successfully");
    }

    @GetMapping("/personality/{userId}")
    @Operation(summary = "Lấy personality hiện tại của user")
    public ResponseEntity<String> getPersonality(@PathVariable String userId) {
        String personality = chatService.getPersonality(userId);
        return ResponseEntity.ok(personality);
    }
}
