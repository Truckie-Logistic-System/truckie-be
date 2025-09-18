package capstone_project.service.mapper.order;

import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.request.order.JourneyHistoryRequest;
import capstone_project.dtos.request.order.UpdateJourneyHistoryRequest;
import capstone_project.dtos.response.order.JourneyHistoryResponse;
import capstone_project.entity.order.order.JourneyHistoryEntity;
import capstone_project.entity.order.order.OrderEntity;
import capstone_project.repository.entityServices.order.order.OrderEntityService;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

@Mapper(componentModel = "spring")
public abstract class JourneyHistoryMapper {

    @Autowired protected OrderEntityService orderService;

    @Mapping(target = "orderEntity", source = "orderId", qualifiedByName = "orderFromId")
    public abstract JourneyHistoryEntity toEntity(JourneyHistoryRequest req);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "orderEntity", source = "orderId", qualifiedByName = "orderFromId")
    public abstract void toEntity(UpdateJourneyHistoryRequest req, @MappingTarget JourneyHistoryEntity entity);

    @Mapping(target = "orderId", source = "orderEntity.id")
    public abstract JourneyHistoryResponse toResponse(JourneyHistoryEntity entity);

    @Named("orderFromId")
    protected OrderEntity orderFromId(String id) {
        if (id == null || id.trim().isEmpty()) {
            return null;
        }
        try {
            return orderService.findEntityById(UUID.fromString(id))
                    .orElseThrow(() -> new NotFoundException(
                            ErrorEnum.NOT_FOUND.getMessage(),
                            ErrorEnum.NOT_FOUND.getErrorCode()));
        } catch (IllegalArgumentException e) {
            throw new NotFoundException(
                    "Invalid UUID format for orderId: " + id,
                    ErrorEnum.INVALID.getErrorCode());
        }
    }
}
