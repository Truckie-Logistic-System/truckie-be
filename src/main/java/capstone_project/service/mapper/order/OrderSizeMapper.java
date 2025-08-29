package capstone_project.service.mapper.order;

import capstone_project.dtos.response.order.GetOrderSizeResponse;
import capstone_project.entity.order.order.OrderSizeEntity;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderSizeMapper {
    GetOrderSizeResponse toOrderSizeResponse(final OrderSizeEntity orderSizeEntity);

    List<GetOrderSizeResponse> toOrderSizeResponseList(final List<OrderSizeEntity> orderSizeEntities);
}
