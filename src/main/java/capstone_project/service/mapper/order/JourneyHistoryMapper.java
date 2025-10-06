package capstone_project.service.mapper.order;

import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.request.order.JourneyHistoryRequest;
import capstone_project.dtos.request.order.UpdateJourneyHistoryRequest;
import capstone_project.dtos.response.order.JourneyHistoryResponse;
import capstone_project.dtos.response.order.JourneySegmentResponse;
import capstone_project.entity.order.order.JourneyHistoryEntity;
import capstone_project.entity.order.order.JourneySegmentEntity;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.repository.entityServices.vehicle.VehicleAssignmentEntityService;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public abstract class JourneyHistoryMapper {

    @Autowired protected VehicleAssignmentEntityService vehicleAssignmentEntityService;

    @Mapping(target = "vehicleAssignment", source = "orderId", qualifiedByName = "vehicleAssignmentFromId")
    public abstract JourneyHistoryEntity toEntity(JourneyHistoryRequest req);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "vehicleAssignment", source = "orderId", qualifiedByName = "vehicleAssignmentFromId")
    public abstract void toEntity(UpdateJourneyHistoryRequest req, @MappingTarget JourneyHistoryEntity entity);

    @Mapping(target = "vehicleAssignmentId", source = "vehicleAssignment.id")
    @Mapping(target = "journeySegments", source = "journeySegments", qualifiedByName = "mapJourneySegments")
    @Mapping(target = "totalDistance", expression = "java(calculateTotalDistance(entity))")
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

    @Named("calculateTotalDistance")
    protected Double calculateTotalDistance(JourneyHistoryEntity entity) {
        if (entity == null || entity.getJourneySegments() == null || entity.getJourneySegments().isEmpty()) {
            return 0.0;
        }

        return entity.getJourneySegments().stream()
                .mapToDouble(segment -> segment.getDistanceMeters() != null ? segment.getDistanceMeters() : 0.0)
                .sum();
    }

    @Named("mapJourneySegments")
    protected List<JourneySegmentResponse> mapJourneySegments(List<JourneySegmentEntity> journeySegments) {
        if (journeySegments == null) {
            return null;
        }

        return journeySegments.stream()
                .map(this::toJourneySegmentResponse)
                .collect(Collectors.toList());
    }

    protected JourneySegmentResponse toJourneySegmentResponse(JourneySegmentEntity entity) {
        if (entity == null) {
            return null;
        }

        return new JourneySegmentResponse(
                entity.getId(),
                entity.getSegmentOrder(),
                entity.getStartPointName(),
                entity.getEndPointName(),
                entity.getStartLatitude(),
                entity.getStartLongitude(),
                entity.getEndLatitude(),
                entity.getEndLongitude(),
                entity.getDistanceMeters(),
                entity.getPathCoordinatesJson(),
                entity.getTollDetailsJson(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getModifiedAt()
        );
    }
}
