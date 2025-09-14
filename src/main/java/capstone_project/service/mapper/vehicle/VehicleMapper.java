package capstone_project.service.mapper.vehicle;

import capstone_project.dtos.request.vehicle.UpdateVehicleRequest;
import capstone_project.dtos.request.vehicle.VehicleRequest;
import capstone_project.dtos.response.vehicle.*;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.entity.vehicle.VehicleEntity;
import capstone_project.entity.vehicle.VehicleMaintenanceEntity;
import capstone_project.entity.vehicle.VehicleTypeEntity;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface VehicleMapper {

    @Mapping(source = "vehicleTypeEntity.id", target = "vehicleTypeId")
    VehicleResponse toVehicleResponse(VehicleEntity entity);

    @Mapping(source = "vehicleTypeEntity.id", target = "vehicleTypeResponse.id")
    VehicleGetDetailsResponse toVehicleDetailResponse(VehicleEntity entity);


    // Add these methods to handle nested object mapping
    VehicleAssignmentResponse toVehicleAssignmentResponse(VehicleAssignmentEntity entity);
    VehicleMaintenanceResponse toVehicleMaintenanceResponse(VehicleMaintenanceEntity entity);
    VehicleTypeResponse toVehicleTypeResponse(VehicleTypeEntity entity);



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