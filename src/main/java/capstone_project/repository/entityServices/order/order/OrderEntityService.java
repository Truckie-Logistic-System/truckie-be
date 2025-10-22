package capstone_project.repository.entityServices.order.order;

import capstone_project.entity.order.order.OrderDetailEntity;
import capstone_project.entity.order.order.OrderEntity;
import capstone_project.repository.entityServices.common.BaseEntityService;

import java.util.List;
import java.util.Optional;
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

    /**
     * Find order associated with a vehicle assignment
     * @param assignmentId the UUID of the vehicle assignment
     * @return Optional containing the order if found, or empty if not found
     */
    Optional<OrderEntity> findVehicleAssignmentOrder(UUID assignmentId);

    List<Object[]> topSenderByMonthAndYear(Integer month, Integer year, int amount);

//    List<Object[]> topReceiverByMonthAndYear(Integer month, Integer year, int amount);

    List<Object[]> topDriverByMonthAndYear(Integer month, Integer year, int amount);

    /**
     * Find an order by its unique order code
     * @param orderCode the order code string
     * @return Optional containing the order if found, empty if not found
     */
    Optional<OrderEntity> findByOrderCode(String orderCode);
    
    /**
     * Find order detail by vehicle assignment ID
     * @param vehicleAssignmentId the UUID of the vehicle assignment
     * @return Optional containing the order detail if found, empty if not found
     */
    Optional<OrderDetailEntity> findOrderDetailByVehicleAssignmentId(UUID vehicleAssignmentId);
}
