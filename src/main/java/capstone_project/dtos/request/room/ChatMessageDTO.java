package capstone_project.dtos.request.room;

public record ChatMessageDTO(
        String id,
        String senderId,
        String content,
        Long createAt,
        String type
) {
}
