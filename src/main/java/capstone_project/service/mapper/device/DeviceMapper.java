package capstone_project.service.mapper.device;


import capstone_project.dtos.request.device.DeviceRequest;
import capstone_project.dtos.request.device.UpdateDeviceRequest;
import capstone_project.dtos.response.device.DeviceResponse;
import capstone_project.entity.device.DeviceEntity;
import capstone_project.entity.device.DeviceTypeEntity;
import capstone_project.entity.vehicle.VehicleEntity;
import org.mapstruct.*;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface DeviceMapper {
//    @Mapping(source = "deviceEntity.deviceTypeEntity.id", target = "deviceTypeResponse.id")
//    @Mapping(source = "vehicleEntity.id", target = "vehicleId")
    DeviceResponse toDeviceResponse(final DeviceEntity deviceEntity);

    @Mapping(target = "deviceTypeEntity", source = "deviceTypeId", qualifiedByName = "deviceTypeFromId")
    @Mapping(target = "vehicleEntity", source = "vehicleId", qualifiedByName = "vehicleFromId")
    @Mapping(target = "installedAt", expression = "java(deviceRequest.installedAt() != null ? deviceRequest.installedAt().atStartOfDay() : null)")
    DeviceEntity mapRequestToEntity(final DeviceRequest deviceRequest);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "deviceTypeEntity", source = "deviceTypeId", qualifiedByName = "deviceTypeFromId")
    @Mapping(target = "vehicleEntity", source = "vehicleId", qualifiedByName = "vehicleFromId")
    @Mapping(target = "installedAt", expression = "java(request.installedAt() != null ? request.installedAt().atStartOfDay() : entity.getInstalledAt())")
    void toDeviceEntity(UpdateDeviceRequest request, @MappingTarget DeviceEntity entity);

    @Named("deviceTypeFromId")
    default DeviceTypeEntity mapDeviceTypeFromId(String deviceTypeId) {
        DeviceTypeEntity entity = new DeviceTypeEntity();
        entity.setId(UUID.fromString(deviceTypeId));
        return entity;
    }

    @Named("vehicleFromId")
    default VehicleEntity mapVehicleFromId(String vehicleId) {
        VehicleEntity entity = new VehicleEntity();
        entity.setId(UUID.fromString(vehicleId));
        return entity;
    }
}
