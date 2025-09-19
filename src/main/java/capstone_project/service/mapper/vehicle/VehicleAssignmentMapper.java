package capstone_project.service.mapper.vehicle;

import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.request.vehicle.UpdateVehicleAssignmentRequest;
import capstone_project.dtos.request.vehicle.VehicleAssignmentRequest;
import capstone_project.dtos.response.vehicle.GetVehicleAssignmentForBillOfLandingResponse;
import capstone_project.dtos.response.vehicle.VehicleAssignmentResponse;
import capstone_project.entity.user.driver.DriverEntity;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.entity.vehicle.VehicleEntity;
import capstone_project.repository.entityServices.user.DriverEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleEntityService;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

@Mapper(componentModel = "spring")
public abstract class VehicleAssignmentMapper {

    @Autowired protected VehicleEntityService  vehicleService;
    @Autowired protected DriverEntityService   driverService;

    @Mapping(target = "vehicleEntity", source = "vehicleId", qualifiedByName = "vehicleFromId")
    @Mapping(target = "driver1", source = "driverId_1", qualifiedByName = "driverFromId")
    @Mapping(target = "driver2", source = "driverId_2", qualifiedByName = "driverFromId")
    public abstract VehicleAssignmentEntity toEntity(VehicleAssignmentRequest req);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "vehicleEntity", source = "vehicleId", qualifiedByName = "vehicleFromId")
    @Mapping(target = "driver2", source = "driverId", qualifiedByName = "driverFromId")
    public abstract void toEntity(UpdateVehicleAssignmentRequest req,
                                  @MappingTarget VehicleAssignmentEntity entity);

    @Mapping(target = "vehicleId", source = "vehicleEntity.id")
    @Mapping(target = "driver_id_1",  source = "driver1.id")
    @Mapping(target = "driver_id_2",  source = "driver2.id")
    public abstract VehicleAssignmentResponse toResponse(VehicleAssignmentEntity entity);

    public abstract GetVehicleAssignmentForBillOfLandingResponse toGetVehicleAssignmentForBillOfLandingResponse(VehicleAssignmentEntity entity);

    /* helpers */
    @Named("vehicleFromId")
    protected VehicleEntity vehicleFromId(String id){
        if(id==null) return null;
        return vehicleService.findEntityById(UUID.fromString(id))
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage(), ErrorEnum.NOT_FOUND.getErrorCode()));
    }
    @Named("driverFromId")
    protected DriverEntity driverFromId(String id){
        if(id==null) return null;
        return driverService.findEntityById(UUID.fromString(id))
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage(), ErrorEnum.NOT_FOUND.getErrorCode()));
    }
}