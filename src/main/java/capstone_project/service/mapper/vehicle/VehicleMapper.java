package capstone_project.service.mapper.vehicle;

import capstone_project.dtos.request.vehicle.UpdateVehicleRequest;
import capstone_project.dtos.request.vehicle.VehicleRequest;
import capstone_project.dtos.response.vehicle.VehicleResponse;
import capstone_project.entity.vehicle.VehicleEntity;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface VehicleMapper {

    @Mapping(source = "vehicleTypeEntity.id", target = "vehicleTypeId")
    VehicleResponse toVehicleResponse(VehicleEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "modifiedAt", ignore = true)
    @Mapping(source = "vehicleTypeId", target = "vehicleTypeEntity.id")
    VehicleEntity toVehicleEntity(VehicleRequest request);



    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
//    @Mapping(source = "vehicleTypeId", target = "vehicleTypeEntity.id")
    @Mapping(source = "currentLatitude",  target = "currentLatitude")
    @Mapping(source = "currentLongitude", target = "currentLongitude")
    void toVehicleEntity(UpdateVehicleRequest req,
                         @MappingTarget VehicleEntity entity);
}