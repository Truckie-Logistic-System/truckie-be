package capstone_project.service.mapper.order;

import capstone_project.dtos.response.order.seal.GetOrderSealResponse;
import capstone_project.entity.order.order.OrderSealEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderSealMapper {
    @Mapping(source = "id", target = "sealId")
    @Mapping(source = "sealCode", target = "sealCode")
    @Mapping(source = "sealAttachedImage", target = "sealAttachedImage")
    @Mapping(source = "sealRemovalTime", target = "sealRemovalTime")
    @Mapping(source = "sealRemovalReason", target = "sealRemovalReason")
    GetOrderSealResponse toGetOrderSealResponse(OrderSealEntity orderSealEntity);

    List<GetOrderSealResponse> toGetOrderSealResponses(List<OrderSealEntity> orderSealEntities);
}
