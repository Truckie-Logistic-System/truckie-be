package capstone_project.service.mapper.setting;

import capstone_project.dtos.request.setting.StipulationSettingRequest;
import capstone_project.dtos.response.setting.StipulationSettingResponse;
import capstone_project.entity.setting.StipulationSettingEntity;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface StipulationSettingMapper {

    StipulationSettingResponse toStipulationSettingResponse(final StipulationSettingEntity stipulationSettingEntity);

    StipulationSettingEntity mapRequestToEntity(final StipulationSettingRequest stipulationSettingRequest);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void toStipulationSettingEntity(StipulationSettingRequest request, @MappingTarget StipulationSettingEntity entity);
}
