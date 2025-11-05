package capstone_project.service.mapper.order;

import capstone_project.dtos.request.pricing.BasingPriceRequest;
import capstone_project.dtos.request.pricing.UpdateBasingPriceRequest;
import capstone_project.dtos.response.pricing.BasingPriceResponse;
import capstone_project.dtos.response.pricing.GetBasingPriceNoSizeRuleResponse;
import capstone_project.dtos.response.pricing.GetBasingPriceResponse;
import capstone_project.entity.pricing.BasingPriceEntity;
import capstone_project.entity.pricing.DistanceRuleEntity;
import capstone_project.entity.pricing.SizeRuleEntity;
import org.mapstruct.*;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface BasingPriceMapper {

    @Mapping(source = "distanceRuleEntity.id", target = "distanceRuleId")
    @Mapping(source = "sizeRuleEntity.id", target = "sizeRuleId")
    BasingPriceResponse toBasingPriceResponse(final BasingPriceEntity basingPriceEntity);

    @Mapping(source = "distanceRuleEntity", target = "distanceRuleResponse")
    @Mapping(source = "sizeRuleEntity", target = "sizeRuleResponse")
    GetBasingPriceResponse toGetBasingPriceResponse(final BasingPriceEntity basingPriceEntity);

    @Mapping(source = "distanceRuleEntity", target = "distanceRuleResponse")
    GetBasingPriceNoSizeRuleResponse toGetBasingPriceNoSizeRuleResponse(final BasingPriceEntity basingPriceEntity);

    @Mapping(target = "distanceRuleEntity", source = "distanceRuleId", qualifiedByName = "distanceRuleFromId")
    @Mapping(target = "sizeRuleEntity", source = "sizeRuleId", qualifiedByName = "sizeRuleFromId")
    BasingPriceEntity mapRequestToEntity(final BasingPriceRequest basingPriceRequest);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "distanceRuleEntity", source = "distanceRuleId", qualifiedByName = "distanceRuleFromId")
    @Mapping(target = "sizeRuleEntity", source = "sizeRuleId", qualifiedByName = "sizeRuleFromId")
    void toBasingPriceEntity(UpdateBasingPriceRequest request, @MappingTarget BasingPriceEntity entity);

    @Named("distanceRuleFromId")
    default DistanceRuleEntity mapDistanceRuleFromId(String distanceRuleId) {
        DistanceRuleEntity entity = new DistanceRuleEntity();
        entity.setId(UUID.fromString(distanceRuleId));
        return entity;
    }

    @Named("sizeRuleFromId")
    default SizeRuleEntity mapSizeRuleFromId(String sizeRuleId) {
        SizeRuleEntity entity = new SizeRuleEntity();
        entity.setId(UUID.fromString(sizeRuleId));
        return entity;
    }
}
