package capstone_project.dtos.request.room;

public record MessageRequest(
         String roomId,    // ID của phòng chat
         String senderId,   // ID của người gửi
         String message,   // Nội dung tin nhắn
         String type
) {
}
