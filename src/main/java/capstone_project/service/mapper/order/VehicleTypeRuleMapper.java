package capstone_project.service.mapper.order;

import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.request.pricing.UpdateVehicleTypeRuleRequest;
import capstone_project.dtos.request.pricing.VehicleTypeRuleRequest;
import capstone_project.dtos.response.pricing.FullVehicleTypeRuleResponse;
import capstone_project.dtos.response.pricing.GetBasingPriceNoVehicleTypeRuleResponse;
import capstone_project.dtos.response.pricing.VehicleTypeRuleResponse;
import capstone_project.entity.order.order.CategoryEntity;
import capstone_project.entity.pricing.VehicleTypeRuleEntity;
import capstone_project.entity.vehicle.VehicleTypeEntity;
import capstone_project.repository.entityServices.order.order.CategoryEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleTypeEntityService;
import capstone_project.service.mapper.vehicle.VehicleTypeMapper;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

//@Mapper(componentModel = "spring", uses = {CategoryMapper.class, VehicleTypeMapper.class})
@Mapper(componentModel = "spring", uses = {CategoryMapper.class, VehicleTypeMapper.class, BasingPriceMapper.class})
public abstract class VehicleTypeRuleMapper {

    @Autowired
    protected CategoryEntityService categoryService;

    @Autowired
    protected VehicleTypeEntityService vehicleTypeEntityService;

    @Mapping(target = "category", source = "categoryId", qualifiedByName = "categoryFromId")
    @Mapping(target = "vehicleTypeEntity", source = "vehicleTypeId", qualifiedByName = "vehicleTypeFromId")
    public abstract VehicleTypeRuleEntity mapRequestToEntity(VehicleTypeRuleRequest request);

    @Named("categoryFromId")
    protected CategoryEntity mapCategoryIdToCategoryEntity(String categoryId) {
        return categoryService.findEntityById(UUID.fromString(categoryId))
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));
    }

    @Named("vehicleTypeFromId")
    protected VehicleTypeEntity mapVehicleTypeIdToVehicleTypeEntity(String vehicleTypeId) {
        return vehicleTypeEntityService.findEntityById(UUID.fromString(vehicleTypeId))
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));
    }

//    @Mapping(source = "category", target = "categoryResponse")
//    @Mapping(source = "vehicleTypeEntity", target = "vehicleTypeResponse")
    public abstract VehicleTypeRuleResponse toVehicleTypeRuleResponse(final VehicleTypeRuleEntity vehicleTypeRuleEntity);

    @Mapping(target = "id", source = "entity.id")
    @Mapping(target = "basingPrices", expression = "java(basingPriceResponses)")
    public abstract FullVehicleTypeRuleResponse toFullVehicleTypeRuleResponse(
            VehicleTypeRuleEntity entity,
            List<GetBasingPriceNoVehicleTypeRuleResponse> basingPriceResponses
    );

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "category", source = "categoryId", qualifiedByName = "categoryFromId")
    @Mapping(target = "vehicleTypeEntity", source = "vehicleTypeId", qualifiedByName = "vehicleTypeFromId")
    public abstract void toVehicleTypeRuleEntity(UpdateVehicleTypeRuleRequest request, @MappingTarget VehicleTypeRuleEntity entity);

//    protected abstract CategoryResponse toCategoryResponse(CategoryEntity entity);
//
//    protected abstract VehicleTypeResponse toVehicleTypeResponse(VehicleTypeEntity entity);

}


