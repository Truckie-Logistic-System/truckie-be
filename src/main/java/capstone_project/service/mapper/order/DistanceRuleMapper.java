package capstone_project.service.mapper.order;

import capstone_project.dtos.request.pricing.DistanceRuleRequest;
import capstone_project.dtos.request.pricing.UpdateDistanceRuleRequest;
import capstone_project.dtos.response.pricing.DistanceRuleResponse;
import capstone_project.entity.pricing.DistanceRuleEntity;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", uses = {SizeRuleMapper.class})
public interface DistanceRuleMapper {

    DistanceRuleResponse toDistanceRuleResponse(final DistanceRuleEntity distanceRuleEntity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "modifiedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "modifiedBy", ignore = true)
    @Mapping(target = "isDemoData", ignore = true)
    @Mapping(target = "displayOrder", ignore = true)
    @Mapping(target = "displayName", ignore = true)
    @Mapping(target = "isBasePrice", ignore = true)
    @Mapping(target = "status", ignore = true)
    DistanceRuleEntity mapRequestToEntity(final DistanceRuleRequest distanceRuleRequest);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "modifiedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "modifiedBy", ignore = true)
    @Mapping(target = "isDemoData", ignore = true)
    @Mapping(target = "displayOrder", ignore = true)
    @Mapping(target = "displayName", ignore = true)
    @Mapping(target = "isBasePrice", ignore = true)
    @Mapping(target = "status", ignore = true)
    void toDistanceRuleEntity(UpdateDistanceRuleRequest request, @MappingTarget DistanceRuleEntity entity);

//    @Mapping(source = "pricingRuleEntity", target = "sizeRuleResponse")
//    BasingPriceResponse toGetPricingTierResponse(final DistanceRuleEntity distanceRuleEntity);
//
//    @Mapping(source = "pricingRuleEntity.id", target = "pricingRuleId")
//    DistanceRuleResponse toPricingTierResponse(final DistanceRuleEntity distanceRuleEntity);
//
//    @Mapping(target = "pricingRuleEntity", source = "pricingRuleID", qualifiedByName = "pricingRuleFromID")
//    DistanceRuleEntity mapRequestToEntity(final PricingTierRequest pricingTierRequest);
//
//    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
//    @Mapping(target = "pricingRuleEntity", source = "pricingRuleID", qualifiedByName = "pricingRuleFromID")
//    void toPricingTierEntity(PricingTierRequest request, @MappingTarget DistanceRuleEntity entity);
//
//    @Named("pricingRuleFromID")
//    default sizeRuleEntity mapPricingRuleFromId(String pricingRuleId) {
//        sizeRuleEntity entity = new sizeRuleEntity();
//        entity.setId(UUID.fromString(pricingRuleId));
//        return entity;
//    }
}
