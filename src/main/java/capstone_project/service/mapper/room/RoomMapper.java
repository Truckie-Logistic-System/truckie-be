package capstone_project.service.mapper.room;

import capstone_project.dtos.response.room.CreateRoomResponse;
import capstone_project.dtos.response.room.ParticipantResponse;
import capstone_project.entity.chat.ParticipantInfo;
import capstone_project.entity.chat.RoomEntity;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RoomMapper {
    CreateRoomResponse toCreateRoomResponse(RoomEntity roomEntity);

    List<CreateRoomResponse> toCreateRoomResponseList(List<RoomEntity> roomEntities);

    ParticipantResponse toParticipantResponse(ParticipantInfo participantInfo);
}