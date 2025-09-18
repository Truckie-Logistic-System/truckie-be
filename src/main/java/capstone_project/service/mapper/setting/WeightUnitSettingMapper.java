package capstone_project.service.mapper.setting;

import capstone_project.dtos.request.setting.UpdateWeightUnitSettingRequest;
import capstone_project.dtos.request.setting.WeightUnitSettingRequest;
import capstone_project.dtos.response.setting.WeightUnitSettingResponse;
import capstone_project.entity.setting.WeightUnitSettingEntity;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface WeightUnitSettingMapper {
    WeightUnitSettingResponse toWeightUnitSettingResponse(final WeightUnitSettingEntity weightUnitSettingEntity);

    WeightUnitSettingEntity mapRequestToEntity(final WeightUnitSettingRequest weightUnitSettingRequest);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void toWeightUnitSettingEntity(UpdateWeightUnitSettingRequest request, @MappingTarget WeightUnitSettingEntity entity);
}
