package capstone_project.repository.entityServices.order.order;

import capstone_project.entity.order.order.OrderEntity;
import capstone_project.repository.entityServices.common.BaseEntityService;

import java.util.List;
import java.util.UUID;

public interface OrderEntityService extends BaseEntityService<OrderEntity, UUID> {
    List<OrderEntity> findBySenderId(UUID senderId);

    List<OrderEntity> findByDeliveryAddressId(UUID deliveryAddressId);

    /**
     * Find recent orders by customer ID with limit
     * @param customerId customer ID
     * @param limit maximum number of results to return
     * @return list of recent orders with valid receiver information
     */
    List<OrderEntity> findRecentOrdersByCustomerId(UUID customerId, int limit);

    int countAllOrderEntities();

    int countOrderEntitiesBySenderId(UUID senderId);

    int countOrderEntitiesBySenderCompanyNameContainingIgnoreCase(String senderCompanyName);

    int countOrderEntitiesByReceiverNameContainingIgnoreCase(String receiverName);

    List<Object[]> countTotalOrderByMonthOverYear(int year);

    List<Object[]> countAllByOrderStatus();

    List<Object[]> countByOrderStatus(String status);

    List<Object[]> countOrderByWeek(int amount);

    List<Object[]> countOrderByYear(int amount);

    List<OrderEntity> findOrdersByDriverId(UUID driverId);
}
