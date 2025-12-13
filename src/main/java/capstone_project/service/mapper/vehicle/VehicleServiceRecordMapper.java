package capstone_project.service.mapper.vehicle;

import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.enums.VehicleServiceStatusEnum;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.request.vehicle.UpdateVehicleServiceRecordRequest;
import capstone_project.dtos.request.vehicle.VehicleServiceRecordRequest;
import capstone_project.dtos.response.vehicle.VehicleServiceRecordResponse;
import capstone_project.entity.vehicle.VehicleEntity;
import capstone_project.entity.vehicle.VehicleServiceRecordEntity;
import capstone_project.repository.entityServices.vehicle.VehicleEntityService;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

@Mapper(componentModel = "spring", uses = {VehicleMapper.class})
public abstract class VehicleServiceRecordMapper {

    @Autowired protected VehicleEntityService vehicleEntityService;
    @Autowired protected VehicleMapper vehicleMapper;

    /* CREATE */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "modifiedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "modifiedBy", ignore = true)
    @Mapping(target = "serviceStatus", source = "serviceStatus", qualifiedByName = "stringToServiceStatus")
    @Mapping(target = "vehicleEntity", source = "vehicleId", qualifiedByName = "vehicleFromId")
    public abstract VehicleServiceRecordEntity toEntity(VehicleServiceRecordRequest req);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "modifiedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "modifiedBy", ignore = true)
    @Mapping(target = "serviceStatus", source = "serviceStatus", qualifiedByName = "stringToServiceStatus")
    @Mapping(target = "vehicleEntity", source = "vehicleId", qualifiedByName = "vehicleFromId")
    public abstract void toEntity(UpdateVehicleServiceRecordRequest req,
                                  @MappingTarget VehicleServiceRecordEntity entity);

    /* RESPONSE */
    @Mapping(target = "serviceType", source = "serviceType")
    @Mapping(target = "serviceStatus", expression = "java(entity.getServiceStatus() != null ? entity.getServiceStatus().name() : null)")
    @Mapping(target = "vehicleEntity", expression = "java(entity.getVehicleEntity() != null ? vehicleMapper.toResponse(entity.getVehicleEntity()) : null)")
    public abstract VehicleServiceRecordResponse toResponse(VehicleServiceRecordEntity entity);

    /* Helpers */
    @Named("vehicleFromId")
    protected VehicleEntity vehicleFromId(String id) {
        if (id == null) return null;
        return vehicleEntityService.findEntityById(UUID.fromString(id))
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage(), ErrorEnum.NOT_FOUND.getErrorCode()));
    }

    @Named("stringToServiceStatus")
    protected VehicleServiceStatusEnum stringToServiceStatus(String status) {
        if (status == null) return null;
        try {
            return VehicleServiceStatusEnum.valueOf(status);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
