package capstone_project.dtos.request.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatMessageRequest(
        @NotBlank(message = "Message không được để trống")
        @Size(max = 2000, message = "Message không được vượt quá 2000 ký tự")
        String message,

        String sessionId,

        // UUID của user đang chat (null nếu guest)
        String userId
) {
}
