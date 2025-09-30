package capstone_project.service.mapper.order;

import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.request.order.JourneyHistoryRequest;
import capstone_project.dtos.request.order.UpdateJourneyHistoryRequest;
import capstone_project.dtos.response.order.JourneyHistoryResponse;
import capstone_project.entity.order.order.JourneyHistoryEntity;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.repository.entityServices.vehicle.VehicleAssignmentEntityService;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

@Mapper(componentModel = "spring")
public abstract class JourneyHistoryMapper {

    @Autowired protected VehicleAssignmentEntityService vehicleAssignmentEntityService;

    @Mapping(target = "vehicleAssignment", source = "orderId", qualifiedByName = "vehicleAssignmentFromId")
    public abstract JourneyHistoryEntity toEntity(JourneyHistoryRequest req);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "vehicleAssignment", source = "orderId", qualifiedByName = "vehicleAssignmentFromId")
    public abstract void toEntity(UpdateJourneyHistoryRequest req, @MappingTarget JourneyHistoryEntity entity);

    public abstract JourneyHistoryResponse toResponse(JourneyHistoryEntity entity);

    @Named("vehicleAssignmentFromId")
    protected VehicleAssignmentEntity vehicleAssignmentFromId(UUID id) {
        if (id == null) {
            return null;
        }
        try {
            return vehicleAssignmentEntityService.findEntityById(id)
                    .orElseThrow(() -> new NotFoundException(
                            "Vehicle assignment not found with ID: " + id,
                            ErrorEnum.NOT_FOUND.getErrorCode()));
        } catch (IllegalArgumentException e) {
            throw new NotFoundException(
                    "Invalid UUID format for vehicleAssignmentId: " + id,
                    ErrorEnum.INVALID.getErrorCode());
        }
    }
}
