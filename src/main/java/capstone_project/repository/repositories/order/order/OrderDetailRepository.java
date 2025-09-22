package capstone_project.repository.repositories.order.order;

import capstone_project.entity.order.order.OrderDetailEntity;
import capstone_project.repository.repositories.common.BaseRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderDetailRepository extends BaseRepository<OrderDetailEntity> {
    List<OrderDetailEntity> findOrderDetailEntitiesByOrderEntityIdOrderByCreatedAtDesc(UUID orderId);

    /**
     * Find an order detail by its tracking code
     *
     * @param trackingCode The tracking code to search for
     * @return The OrderDetailEntity if found
     */
    Optional<OrderDetailEntity> findByTrackingCode(String trackingCode);
}
