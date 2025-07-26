package capstone_project.service.mapper.vehicle;

import capstone_project.dtos.request.vehicle.VehicleTypeRequest;
import capstone_project.dtos.response.vehicle.VehicleTypeResponse;
import capstone_project.entity.vehicle.VehicleTypeEntity;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface VehicleTypeMapper {

    @Mapping(target = "id", source = "vehicleType.id")
    VehicleTypeResponse toVehicleTypeResponse(VehicleTypeEntity vehicleType);

    VehicleTypeEntity mapRequestToVehicleTypeEntity(VehicleTypeRequest vehicleTypeRequest);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void toVehicleEntity(VehicleTypeRequest request, @MappingTarget VehicleTypeEntity entity);
}
