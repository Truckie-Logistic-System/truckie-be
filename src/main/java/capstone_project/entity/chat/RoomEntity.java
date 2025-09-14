package capstone_project.entity.chat;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.ServerTimestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomEntity {
    @DocumentId
    private String roomId;
    private String orderId;
    private List<ParticipantInfo> participants;
    private String type;
    private String status;
    @ServerTimestamp
    private Instant createdAt;

}
