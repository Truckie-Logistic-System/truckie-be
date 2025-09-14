package capstone_project.service.mapper.vehicle;

import capstone_project.dtos.request.vehicle.MaintenanceTypeRequest;
import capstone_project.dtos.request.vehicle.UpdateMaintenanceTypeRequest;
import capstone_project.dtos.response.vehicle.MaintenanceTypeResponse;
import capstone_project.entity.vehicle.MaintenanceTypeEntity;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface MaintenanceTypeMapper {

    @Mapping(target = "id", ignore = true)
//    @Mapping(target = "createdAt", ignore = true)
//    @Mapping(target = "modifiedAt", ignore = true)
//    @Mapping(target = "createdBy", ignore = true)
//    @Mapping(target = "modifiedBy", ignore = true)
    MaintenanceTypeEntity toEntity(MaintenanceTypeRequest req);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void toEntity(UpdateMaintenanceTypeRequest req, @MappingTarget MaintenanceTypeEntity entity);

    MaintenanceTypeResponse toResponse(MaintenanceTypeEntity entity);
}
