package capstone_project.service.mapper.order;

import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.request.pricing.UpdateVehicleRuleRequest;
import capstone_project.dtos.request.pricing.VehicleRuleRequest;
import capstone_project.dtos.response.pricing.FullVehicleRuleResponse;
import capstone_project.dtos.response.pricing.GetBasingPriceNoVehicleRuleResponse;
import capstone_project.dtos.response.pricing.VehicleRuleResponse;
import capstone_project.entity.order.order.CategoryEntity;
import capstone_project.entity.pricing.VehicleRuleEntity;
import capstone_project.entity.vehicle.VehicleTypeEntity;
import capstone_project.repository.entityServices.order.order.CategoryEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleTypeEntityService;
import capstone_project.service.mapper.vehicle.VehicleTypeMapper;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

//@Mapper(componentModel = "spring", uses = {CategoryMapper.class, VehicleTypeMapper.class})
@Mapper(componentModel = "spring", uses = {CategoryMapper.class, VehicleTypeMapper.class, BasingPriceMapper.class})
public abstract class VehicleRuleMapper {

    @Autowired
    protected CategoryEntityService categoryService;

    @Autowired
    protected VehicleTypeEntityService vehicleTypeEntityService;

    @Mapping(target = "category", source = "categoryId", qualifiedByName = "categoryFromId")
    @Mapping(target = "vehicleTypeEntity", source = "vehicleTypeId", qualifiedByName = "vehicleTypeFromId")
    public abstract VehicleRuleEntity mapRequestToEntity(VehicleRuleRequest request);

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
    public abstract VehicleRuleResponse toVehicleRuleResponse(final VehicleRuleEntity vehicleRuleEntity);

    @Mapping(target = "id", source = "entity.id")
    @Mapping(target = "basingPrice", expression = "java(getBasingPriceNoVehicleRuleResponse)")
    public abstract FullVehicleRuleResponse toFullVehicleRuleResponse(
            VehicleRuleEntity entity,
            GetBasingPriceNoVehicleRuleResponse getBasingPriceNoVehicleRuleResponse
    );

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "category", source = "categoryId", qualifiedByName = "categoryFromId")
    @Mapping(target = "vehicleTypeEntity", source = "vehicleTypeId", qualifiedByName = "vehicleTypeFromId")
    public abstract void toVehicleRuleEntity(UpdateVehicleRuleRequest request, @MappingTarget VehicleRuleEntity entity);

//    protected abstract CategoryResponse toCategoryResponse(CategoryEntity entity);
//
//    protected abstract VehicleTypeResponse toVehicleTypeResponse(VehicleTypeEntity entity);

}


