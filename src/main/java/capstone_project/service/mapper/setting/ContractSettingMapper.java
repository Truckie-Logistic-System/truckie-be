package capstone_project.service.mapper.setting;

import capstone_project.dtos.request.setting.ContractSettingRequest;
import capstone_project.dtos.request.setting.UpdateContractSettingRequest;
import capstone_project.dtos.response.setting.ContractSettingResponse;
import capstone_project.entity.setting.ContractSettingEntity;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface ContractSettingMapper {
    ContractSettingResponse toContractSettingResponse(final ContractSettingEntity contractSettingEntity);

    ContractSettingEntity mapRequestToEntity(final ContractSettingRequest contractSettingRequest);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void toContractSettingEntity(UpdateContractSettingRequest request, @MappingTarget ContractSettingEntity entity);
}
