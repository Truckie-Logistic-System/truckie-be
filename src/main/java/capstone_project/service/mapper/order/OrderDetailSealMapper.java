package capstone_project.service.mapper.order;

import capstone_project.dtos.response.order.seal.GetOrderDetailSealResponse;
import capstone_project.entity.order.order.OrderDetailSealEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderDetailSealMapper {
    @Mapping(source = "seal.id", target = "sealId")
    GetOrderDetailSealResponse toGetOrderDetailSealResponse(OrderDetailSealEntity orderDetailSealEntity);

    List<GetOrderDetailSealResponse> toGetOrderDetailSealResponses(List<OrderDetailSealEntity> orderDetailSealEntities);
}
