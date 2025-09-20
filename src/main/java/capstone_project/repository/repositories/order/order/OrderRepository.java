package capstone_project.repository.repositories.order.order;

import capstone_project.entity.order.order.OrderEntity;
import capstone_project.repository.repositories.common.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface OrderRepository extends BaseRepository<OrderEntity> {
    // Additional methods specific to OrderRepository can be defined here
    List<OrderEntity> findBySenderIdOrderByCreatedAtDesc(UUID senderId);

    List<OrderEntity> findByDeliveryAddressId(UUID deliveryAddressId);

    /**
     * Find recent orders by customer ID with limit
     * Returns orders that have valid receiver information (name and phone)
     * Sorted by creation date (newest first)
     *
     * @param senderId customer/sender ID
     * @param limit maximum number of results to return
     * @return list of recent orders
     */
    @Query(value = "SELECT o.* FROM orders o " +
            "JOIN customers c ON o.customer_id = c.id " +
            "WHERE c.id = :senderId AND o.receiver_name IS NOT NULL " +
            "AND o.receiver_phone IS NOT NULL " +
            "ORDER BY o.created_at DESC LIMIT :limit",
           nativeQuery = true)
    List<OrderEntity> findRecentOrdersByCustomerId(@Param("senderId") UUID senderId, @Param("limit") int limit);
}
