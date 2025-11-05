package capstone_project.service.mapper.order;

import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.request.pricing.SizeRuleRequest;
import capstone_project.dtos.request.pricing.UpdateSizeRuleRequest;
import capstone_project.dtos.response.pricing.FullSizeRuleResponse;
import capstone_project.dtos.response.pricing.GetBasingPriceNoSizeRuleResponse;
import capstone_project.dtos.response.pricing.SizeRuleResponse;
import capstone_project.entity.order.order.CategoryEntity;
import capstone_project.entity.pricing.SizeRuleEntity;
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
public abstract class SizeRuleMapper {

    @Autowired
    protected CategoryEntityService categoryService;

    @Autowired
    protected VehicleTypeEntityService vehicleTypeEntityService;

    @Mapping(target = "category", source = "categoryId", qualifiedByName = "categoryFromId")
    @Mapping(target = "vehicleTypeEntity", source = "vehicleTypeId", qualifiedByName = "vehicleTypeFromId")
    public abstract SizeRuleEntity mapRequestToEntity(SizeRuleRequest request);

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
    public abstract SizeRuleResponse toSizeRuleResponse(final SizeRuleEntity sizeRuleEntity);

    @Mapping(target = "id", source = "entity.id")
    @Mapping(target = "basingPrices", expression = "java(basingPriceResponses)")
    public abstract FullSizeRuleResponse toFullsizeRuleResponse(
            SizeRuleEntity entity,
            List<GetBasingPriceNoSizeRuleResponse> basingPriceResponses
    );

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "category", source = "categoryId", qualifiedByName = "categoryFromId")
    @Mapping(target = "vehicleTypeEntity", source = "vehicleTypeId", qualifiedByName = "vehicleTypeFromId")
    public abstract void toSizeRuleEntity(UpdateSizeRuleRequest request, @MappingTarget SizeRuleEntity entity);

//    protected abstract CategoryResponse toCategoryResponse(CategoryEntity entity);
//
//    protected abstract VehicleTypeResponse toVehicleTypeResponse(VehicleTypeEntity entity);

}


