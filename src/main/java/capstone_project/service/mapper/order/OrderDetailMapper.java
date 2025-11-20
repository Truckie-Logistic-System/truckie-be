package capstone_project.service.mapper.order;

import capstone_project.dtos.response.order.GetOrderDetailResponse;
import capstone_project.dtos.response.order.GetOrderDetailsResponseForList;
import capstone_project.entity.order.order.OrderDetailEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderDetailMapper {
    @Mapping(source = "orderEntity.id", target = "orderId")
    @Mapping(source = "orderSizeEntity", target = "orderSizeId")
    @Mapping(source = "vehicleAssignmentEntity.id", target = "vehicleAssignmentId")
    @Mapping(source = "weightTons", target = "weight")  // Map new entity field to old response field
    GetOrderDetailResponse toGetOrderDetailResponse(OrderDetailEntity entity);

    List<GetOrderDetailResponse> toGetOrderDetailResponseList(List<OrderDetailEntity> entityList);

    @Mapping(source = "orderEntity.id", target = "orderId")
    @Mapping(source = "orderSizeEntity.id", target = "orderSizeId")
    @Mapping(source = "vehicleAssignmentEntity.id", target = "vehicleAssignmentId")
    @Mapping(source = "weightTons", target = "weight")  // Map new entity field to old response field
    GetOrderDetailsResponseForList toGetOrderDetailsResponseForList(OrderDetailEntity entityList);

    List<GetOrderDetailsResponseForList> toGetOrderDetailResponseListBasic(List<OrderDetailEntity> entityList);
}

