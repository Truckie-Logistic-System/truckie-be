package capstone_project.service.mapper.vehicle;

import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.request.vehicle.UpdateVehicleMaintenanceRequest;
import capstone_project.dtos.request.vehicle.VehicleMaintenanceRequest;
import capstone_project.dtos.response.vehicle.VehicleMaintenanceResponse;
import capstone_project.entity.vehicle.MaintenanceTypeEntity;
import capstone_project.entity.vehicle.VehicleEntity;
import capstone_project.entity.vehicle.VehicleMaintenanceEntity;
import capstone_project.service.entityServices.vehicle.MaintenanceTypeEntityService;
import capstone_project.service.entityServices.vehicle.VehicleEntityService;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

@Mapper(componentModel = "spring")
public abstract class VehicleMaintenanceMapper {

    @Autowired protected VehicleEntityService vehicleEntityService;
    @Autowired protected MaintenanceTypeEntityService maintenanceTypeEntityService;

    /* CREATE */
    @Mapping(target = "vehicleEntity",        source = "vehicleId",        qualifiedByName = "vehicleFromId")
    @Mapping(target = "maintenanceTypeEntity", source = "maintenanceTypeId", qualifiedByName = "maintenanceTypeFromId")
    public abstract VehicleMaintenanceEntity toEntity(VehicleMaintenanceRequest req);

    /* UPDATE (null-safe) */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "vehicleEntity",        source = "vehicleId",        qualifiedByName = "vehicleFromId")
    @Mapping(target = "maintenanceTypeEntity", source = "maintenanceTypeId", qualifiedByName = "maintenanceTypeFromId")
    public abstract void toEntity(UpdateVehicleMaintenanceRequest req,
                                  @MappingTarget VehicleMaintenanceEntity entity);

    /* RESPONSE */
    @Mapping(target = "vehicleId",         source = "vehicleEntity.id")
    @Mapping(target = "maintenanceTypeId", source = "maintenanceTypeEntity.id")
    public abstract VehicleMaintenanceResponse toResponse(VehicleMaintenanceEntity entity);

    /* Helpers */
    @Named("vehicleFromId")
    protected VehicleEntity vehicleFromId(String id) {
        if (id == null) return null;
        return vehicleEntityService.findContractRuleEntitiesById(UUID.fromString(id))
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage(), ErrorEnum.NOT_FOUND.getErrorCode()));
    }

    @Named("maintenanceTypeFromId")
    protected MaintenanceTypeEntity maintenanceTypeFromId(String id) {
        if (id == null) return null;
        return maintenanceTypeEntityService.findContractRuleEntitiesById(UUID.fromString(id))
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage(), ErrorEnum.NOT_FOUND.getErrorCode()));
    }
}