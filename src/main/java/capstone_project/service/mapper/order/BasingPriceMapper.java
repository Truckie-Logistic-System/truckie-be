package capstone_project.service.mapper.order;

import capstone_project.dtos.request.pricing.BasingPriceRequest;
import capstone_project.dtos.request.pricing.UpdateBasingPriceRequest;
import capstone_project.dtos.response.pricing.BasingPriceResponse;
import capstone_project.dtos.response.pricing.GetBasingPriceNoVehicleRuleResponse;
import capstone_project.dtos.response.pricing.GetBasingPriceResponse;
import capstone_project.entity.pricing.BasingPriceEntity;
import capstone_project.entity.pricing.DistanceRuleEntity;
import capstone_project.entity.pricing.VehicleRuleEntity;
import org.mapstruct.*;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface BasingPriceMapper {

    @Mapping(source = "distanceRuleEntity.id", target = "distanceRuleId")
    @Mapping(source = "vehicleRuleEntity.id", target = "vehicleRuleId")
    BasingPriceResponse toBasingPriceResponse(final BasingPriceEntity basingPriceEntity);

    @Mapping(source = "distanceRuleEntity", target = "distanceRuleResponse")
    @Mapping(source = "vehicleRuleEntity", target = "vehicleRuleResponse")
    GetBasingPriceResponse toGetBasingPriceResponse(final BasingPriceEntity basingPriceEntity);

    @Mapping(source = "distanceRuleEntity", target = "distanceRuleResponse")
    GetBasingPriceNoVehicleRuleResponse toGetBasingPriceNoVehicleRuleResponse(final BasingPriceEntity basingPriceEntity);

    @Mapping(target = "distanceRuleEntity", source = "distanceRuleId", qualifiedByName = "distanceRuleFromId")
    @Mapping(target = "vehicleRuleEntity", source = "vehicleRuleId", qualifiedByName = "vehicleRuleFromId")
    BasingPriceEntity mapRequestToEntity(final BasingPriceRequest basingPriceRequest);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "distanceRuleEntity", source = "distanceRuleId", qualifiedByName = "distanceRuleFromId")
    @Mapping(target = "vehicleRuleEntity", source = "vehicleRuleId", qualifiedByName = "vehicleRuleFromId")
    void toBasingPriceEntity(UpdateBasingPriceRequest request, @MappingTarget BasingPriceEntity entity);

    @Named("distanceRuleFromId")
    default DistanceRuleEntity mapDistanceRuleFromId(String distanceRuleId) {
        DistanceRuleEntity entity = new DistanceRuleEntity();
        entity.setId(UUID.fromString(distanceRuleId));
        return entity;
    }

    @Named("vehicleRuleFromId")
    default VehicleRuleEntity mapVehicleRuleFromId(String vehicleRuleId) {
        VehicleRuleEntity entity = new VehicleRuleEntity();
        entity.setId(UUID.fromString(vehicleRuleId));
        return entity;
    }
}
