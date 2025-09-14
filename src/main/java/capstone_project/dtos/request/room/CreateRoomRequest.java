package capstone_project.dtos.request.room;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateRoomRequest(
        @NotNull(message = "OrderId is required")
        String orderId,
        @NotNull(message = "Participants is required")
        List<String> userIds
) {
}
