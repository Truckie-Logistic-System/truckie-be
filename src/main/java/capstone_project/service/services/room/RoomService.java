package capstone_project.service.services.room;

import capstone_project.dtos.request.room.CreateRoomRequest;
import capstone_project.dtos.response.room.CreateRoomResponse;

import java.util.List;
import java.util.UUID;

public interface RoomService {
    CreateRoomResponse createRoom(CreateRoomRequest request);

    boolean cancelRoomByOrderId(UUID orderId);

    List<CreateRoomResponse> listRoomActiveByUserId(UUID userId);

    boolean activeRoomByOrderId(UUID orderId);

    boolean deleteRoomByOrderId(UUID orderId);
}
