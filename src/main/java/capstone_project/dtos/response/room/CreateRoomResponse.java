package capstone_project.dtos.response.room;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateRoomResponse(
        @NotNull(message = "RoomId is required")
        String roomId,
        @NotNull(message = "OrderId is required")
        String orderId,
        @NotNull(message = "Participants is required")
        List<ParticipantResponse> participants,
        @NotNull(message = "status is required")
        String status,
        String type
) {
}
