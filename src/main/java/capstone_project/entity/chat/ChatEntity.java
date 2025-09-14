package capstone_project.entity.chat;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.ServerTimestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatEntity {
    @DocumentId
    private String chatId;
    private String roomId;
    private String senderId;
    private String content;
    private String type;
    private String status;
    @ServerTimestamp
    private Instant createdAt;
}