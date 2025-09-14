package capstone_project.dtos.response.room;

import capstone_project.dtos.request.room.ChatMessageDTO;

import java.util.List;

public record ChatPageResponse (
        List<ChatMessageDTO> messages,
        String lastMessageId, // để load trang kế tiếp
        boolean hasMore
) {
}
