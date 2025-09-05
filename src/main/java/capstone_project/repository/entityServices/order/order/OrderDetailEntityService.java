package capstone_project.repository.entityServices.order.order;

import capstone_project.entity.order.order.OrderDetailEntity;
import capstone_project.repository.entityServices.common.BaseEntityService;

import java.util.List;
import java.util.UUID;

public interface OrderDetailEntityService extends BaseEntityService<OrderDetailEntity, UUID> {
    List<OrderDetailEntity> findOrderDetailEntitiesByOrderEntityId(UUID orderDetailEntityId);

    List<OrderDetailEntity> saveAllOrderDetailEntities(List<OrderDetailEntity> orderDetailEntities);

    List<OrderDetailEntity> findAllByIds(List<UUID> ids);


}
