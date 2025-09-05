package capstone_project.repository.repositories.order.order;

import capstone_project.entity.order.order.OrderEntity;
import capstone_project.repository.repositories.common.BaseRepository;

import java.util.List;
import java.util.UUID;

public interface OrderRepository extends BaseRepository<OrderEntity> {
    // Additional methods specific to OrderRepository can be defined here
    List<OrderEntity> findBySenderId(UUID senderId);

    List<OrderEntity> findByDeliveryAddressId(UUID deliveryAddressId);


}
