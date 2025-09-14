package capstone_project.service.mapper.room;

import capstone_project.dtos.response.room.ChatResponseDTO;
import capstone_project.entity.chat.ChatEntity;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ChatMapper {

    ChatResponseDTO toChatResponseDTO(ChatEntity chatEntity);

    List<ChatResponseDTO> toChatResponseDTOList(List<ChatEntity> chatEntities);
}
