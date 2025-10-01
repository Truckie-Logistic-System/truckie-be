package capstone_project.repository.repositories.order.order;

import capstone_project.entity.order.order.OrderEntity;
import capstone_project.repository.repositories.common.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends BaseRepository<OrderEntity> {
    // Additional methods specific to OrderRepository can be defined here
    List<OrderEntity> findBySenderIdOrderByCreatedAtDesc(UUID senderId);

    List<OrderEntity> findByDeliveryAddressId(UUID deliveryAddressId);

    @Query(value = """
            SELECT o.*
            FROM orders o
                     JOIN order_details od ON o.id = od.order_id
                     JOIN vehicle_assignments va ON od.vehicle_assignment_id = va.id
                     LEFT JOIN drivers d1 ON va.driver_id_1 = d1.id
                     LEFT JOIN drivers d2 ON va.driver_id_2 = d2.id
            WHERE d1.id = :driverId
               OR d2.id = :driverId;
            """, nativeQuery = true)
    List<OrderEntity> findOrdersByDriverId(@Param("driverId") UUID driverId);

    /**
     * Find order associated with a vehicle assignment
     * @param assignmentId the UUID of the vehicle assignment
     * @return Optional containing the order if found
     */
    @Query(value = """
            SELECT o.*
            FROM orders o
            JOIN order_details od ON o.id = od.order_id
            JOIN vehicle_assignments va ON od.vehicle_assignment_id = va.id
            WHERE va.id = :assignmentId
            """, nativeQuery = true)
    Optional<OrderEntity> findVehicleAssignmentOrder(@Param("assignmentId") UUID assignmentId);

    /**
     * Find recent orders by customer ID with limit
     * Returns orders that have valid receiver information (name and phone)
     * Sorted by creation date (newest first)
     *
     * @param senderId customer/sender ID
     * @param limit    maximum number of results to return
     *
     * @return list of recent orders
     */
    @Query(value = "SELECT o.* FROM orders o " +
            "JOIN customers c ON o.customer_id = c.id " +
            "WHERE c.id = :senderId AND o.receiver_name IS NOT NULL " +
            "AND o.receiver_phone IS NOT NULL " +
            "ORDER BY o.created_at DESC LIMIT :limit",
            nativeQuery = true)
    List<OrderEntity> findRecentOrdersByCustomerId(@Param("senderId") UUID senderId, @Param("limit") int limit);

    int countOrderEntitiesBySenderId(UUID senderId);

//    @Query("""
//        SELECT new com.example.dto.TopCustomerDTO(c.id, c.name, COUNT(o.id))
//        FROM OrderEntity o
//        JOIN o.customer c
//        WHERE FUNCTION('EXTRACT', MONTH FROM o.createdAt) = :month
//          AND FUNCTION('EXTRACT', YEAR FROM o.createdAt) = :year
//        GROUP BY c.id, c.name
//        ORDER BY COUNT(o.id) DESC
//        """)
//    List<TopCustomerDTO> findTopCustomersByMonth(@Param("month") int month, @Param("year") int year, Pageable pageable);
//
//    @Query("""
//        SELECT new com.example.dto.TopCustomerDTO(c.id, c.name, COUNT(o.id))
//        FROM OrderEntity o
//        JOIN o.customer c
//        WHERE FUNCTION('EXTRACT', YEAR FROM o.createdAt) = :year
//        GROUP BY c.id, c.name
//        ORDER BY COUNT(o.id) DESC
//        """)
//    List<TopCustomerDTO> findTopCustomersByYear(@Param("year") int year, Pageable pageable);

    @Query(value = """
            SELECT  COUNT(*)
            FROM orders;
            """, nativeQuery = true)
    int countAllOrderEntities();

    int countOrderEntitiesBySenderCompanyNameContainingIgnoreCase(String senderCompanyName);

    int countOrderEntitiesByReceiverNameContainingIgnoreCase(String receiverName);

    @Query(value = """
            SELECT
                EXTRACT(MONTH FROM created_at::date) AS month,
                COALESCE(count(*), 0) AS order_count
            FROM orders
            WHERE EXTRACT(YEAR FROM created_at::date) = :year
            GROUP BY month
            ORDER BY month ASC;
            """, nativeQuery = true)
    List<Object[]> countTotalOrderByMonthOverYear(@Param("year") int year);

    @Query(value = """
            SELECT o.status, COUNT(*)
            FROM orders o
            group by o.status;
            """, nativeQuery = true)
    List<Object[]> countAllByOrderStatus();

    @Query(value = """
            SELECT o.status, COUNT(*) as count
            FROM orders o
            WHERE o.status = :status
            group by o.status
            """, nativeQuery = true)
    List<Object[]> countByOrderStatus(@Param("status") String status);

    @Query(value = """
            SELECT 'week' AS period, COUNT(*) AS count
            FROM orders
            WHERE EXTRACT(WEEK FROM created_at) = :amount
              AND EXTRACT(YEAR FROM created_at) = EXTRACT(YEAR FROM CURRENT_DATE);
            """, nativeQuery = true)
    List<Object[]> countOrderByWeek(@Param("amount") int amount);

    @Query(value = """
            SELECT 'year' AS period, COUNT(*) AS count
            FROM orders
            WHERE EXTRACT(YEAR FROM created_at) = :amount;
            """, nativeQuery = true)
    List<Object[]> countOrderByYear(@Param("amount") int amount);


}
