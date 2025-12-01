package capstone_project.dtos.request.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Request DTO for uploading chat image
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadChatImageRequest {
    
    private MultipartFile file;
    private UUID conversationId;
    private UUID senderId;
    private String guestSessionId;
}
