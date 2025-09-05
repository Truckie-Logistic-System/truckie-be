package capstone_project.repository.repositories.order.order;

import capstone_project.entity.order.order.OrderDetailEntity;
import capstone_project.repository.repositories.common.BaseRepository;

import java.util.List;
import java.util.UUID;

public interface OrderDetailRepository extends BaseRepository<OrderDetailEntity> {
    List<OrderDetailEntity> findOrderDetailEntitiesByOrderEntityId(UUID orderId);
}
