package capstone_project.service.mapper.order;

import capstone_project.dtos.request.pricing.BasingPriceRequest;
import capstone_project.dtos.request.pricing.UpdateBasingPriceRequest;
import capstone_project.dtos.response.pricing.BasingPriceResponse;
import capstone_project.dtos.response.pricing.GetBasingPriceNoVehicleTypeRuleResponse;
import capstone_project.dtos.response.pricing.GetBasingPriceResponse;
import capstone_project.entity.pricing.BasingPriceEntity;
import capstone_project.entity.pricing.DistanceRuleEntity;
import capstone_project.entity.pricing.VehicleTypeRuleEntity;
import org.mapstruct.*;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface BasingPriceMapper {

    @Mapping(source = "distanceRuleEntity.id", target = "distanceRuleId")
    @Mapping(source = "vehicleTypeRuleEntity.id", target = "vehicleRuleId")
    BasingPriceResponse toBasingPriceResponse(final BasingPriceEntity basingPriceEntity);

    @Mapping(source = "distanceRuleEntity", target = "distanceRuleResponse")
    @Mapping(source = "vehicleTypeRuleEntity", target = "vehicleTypeRuleResponse")
    GetBasingPriceResponse toGetBasingPriceResponse(final BasingPriceEntity basingPriceEntity);

    @Mapping(source = "distanceRuleEntity", target = "distanceRuleResponse")
    GetBasingPriceNoVehicleTypeRuleResponse toGetBasingPriceNoVehicleTypeRuleResponse(final BasingPriceEntity basingPriceEntity);

    @Mapping(target = "distanceRuleEntity", source = "distanceRuleId", qualifiedByName = "distanceRuleFromId")
    @Mapping(target = "vehicleTypeRuleEntity", source = "vehicleRuleId", qualifiedByName = "vehicleRuleFromId")
    BasingPriceEntity mapRequestToEntity(final BasingPriceRequest basingPriceRequest);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "distanceRuleEntity", source = "distanceRuleId", qualifiedByName = "distanceRuleFromId")
    @Mapping(target = "vehicleTypeRuleEntity", source = "vehicleRuleId", qualifiedByName = "vehicleRuleFromId")
    void toBasingPriceEntity(UpdateBasingPriceRequest request, @MappingTarget BasingPriceEntity entity);

    @Named("distanceRuleFromId")
    default DistanceRuleEntity mapDistanceRuleFromId(String distanceRuleId) {
        DistanceRuleEntity entity = new DistanceRuleEntity();
        entity.setId(UUID.fromString(distanceRuleId));
        return entity;
    }

    @Named("vehicleRuleFromId")
    default VehicleTypeRuleEntity mapVehicleTypeRuleFromId(String vehicleRuleId) {
        VehicleTypeRuleEntity entity = new VehicleTypeRuleEntity();
        entity.setId(UUID.fromString(vehicleRuleId));
        return entity;
    }
}
