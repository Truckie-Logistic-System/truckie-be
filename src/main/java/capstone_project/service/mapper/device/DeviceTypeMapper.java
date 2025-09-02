package capstone_project.service.mapper.device;

import capstone_project.dtos.request.device.DeviceTypeRequest;
import capstone_project.dtos.request.device.UpdateDeviceTypeRequest;
import capstone_project.dtos.response.device.DeviceTypeResponse;
import capstone_project.entity.device.DeviceTypeEntity;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface DeviceTypeMapper {

    DeviceTypeResponse toDeviceTypeResponse(final DeviceTypeEntity deviceTypeEntity);

    DeviceTypeEntity mapRequestToEntity(final DeviceTypeRequest deviceTypeRequest);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void toDeviceTypeEntity(UpdateDeviceTypeRequest request, @MappingTarget DeviceTypeEntity entity);
}
