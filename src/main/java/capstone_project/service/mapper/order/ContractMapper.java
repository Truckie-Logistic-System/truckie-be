package capstone_project.service.mapper.order;

import capstone_project.dtos.request.order.ContractRequest;
import capstone_project.dtos.response.order.contract.ContractResponse;
import capstone_project.entity.auth.UserEntity;
import capstone_project.entity.order.contract.ContractEntity;
import capstone_project.entity.order.order.OrderEntity;
import org.mapstruct.*;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface ContractMapper {

    @Mapping(source = "orderEntity.id", target = "orderId")
    @Mapping(source = "staff.id", target = "staffId")
    ContractResponse toContractResponse(ContractEntity contractEntity);

    @Mapping(source = "orderId", target = "orderEntity", qualifiedByName = "orderFromId")
//    @Mapping(source = "staffId", target = "staff", qualifiedByName = "staffFromId")
    @Mapping(target = "totalValue", ignore = true)
    ContractEntity mapRequestToEntity(ContractRequest contractRequest);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "orderEntity", source = "orderId", qualifiedByName = "orderFromId")
//    @Mapping(target = "staff", source = "staffId", qualifiedByName = "staffFromId")
    void toContractEntity(ContractRequest request, @MappingTarget ContractEntity entity);

    @Named("orderFromId")
    default OrderEntity mapOrderFromId(String orderId) {
        OrderEntity entity = new OrderEntity();
        entity.setId(UUID.fromString(orderId));
        return entity;
    }

    @Named("staffFromId")
    default UserEntity mauserFromId(String userId) {
        UserEntity entity = new UserEntity();
        entity.setId(UUID.fromString(userId));
        return entity;
    }
}

