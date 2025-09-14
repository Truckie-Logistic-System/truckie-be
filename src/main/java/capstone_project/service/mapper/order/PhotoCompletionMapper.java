package capstone_project.service.mapper.order;

import capstone_project.dtos.response.order.PhotoCompletionResponse;
import capstone_project.entity.order.conformation.PhotoCompletionEntity;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PhotoCompletionMapper {
    PhotoCompletionResponse toPhotoCompletionResponse(PhotoCompletionEntity photo);

    List<PhotoCompletionResponse> toPhotoCompletionResponses(List<PhotoCompletionEntity> photos);
}
