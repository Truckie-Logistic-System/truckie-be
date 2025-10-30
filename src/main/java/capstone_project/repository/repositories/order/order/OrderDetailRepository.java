package capstone_project.repository.repositories.order.order;

import capstone_project.entity.order.order.OrderDetailEntity;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.repository.repositories.common.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderDetailRepository extends BaseRepository<OrderDetailEntity> {
    List<OrderDetailEntity> findOrderDetailEntitiesByOrderEntityIdOrderByCreatedAtDesc(UUID orderId);

    /**
     * Find an order detail by its tracking code
     *
     * @param trackingCode The tracking code to search for
     *
     * @return The OrderDetailEntity if found
     */
    Optional<OrderDetailEntity> findByTrackingCode(String trackingCode);

    /**
     * Find active order details by vehicle assignment ID for real-time tracking
     * Check Order status instead of OrderDetail status
     */
    @Query("SELECT od FROM OrderDetailEntity od " +
           "JOIN od.orderEntity o " +
           "WHERE od.vehicleAssignmentEntity.id = :vehicleAssignmentId " +
           "AND o.status IN :statuses")
    List<OrderDetailEntity> findActiveOrderDetailsByVehicleAssignmentId(
            @Param("vehicleAssignmentId") UUID vehicleAssignmentId,
            @Param("statuses") List<String> statuses);

    @Query(value = """
            SELECT SUM(CASE WHEN od.end_time <= od.estimated_end_time THEN 1 ELSE 0 END) AS on_time_count,
                   SUM(CASE WHEN od.end_time > od.estimated_end_time THEN 1 ELSE 0 END)  AS late_count,
                   ROUND(
                           (SUM(CASE WHEN od.end_time <= od.estimated_end_time THEN 1 ELSE 0 END) * 100.0) /
                           NULLIF(COUNT(od.id), 0), 2
                   )                                                                     AS on_time_percentage,
                   ROUND(
                           (SUM(CASE WHEN od.end_time > od.estimated_end_time THEN 1 ELSE 0 END) * 100.0) /
                           NULLIF(COUNT(od.id), 0), 2
                   )                                                                     AS late_percentage
            FROM order_details od
                     JOIN orders o ON od.order_id = o.id
            WHERE od.end_time IS NOT NULL
              AND (:year IS NULL OR EXTRACT(YEAR FROM o.created_at) = :year)
              AND (:month IS NULL OR EXTRACT(MONTH FROM o.created_at) = :month);
            """, nativeQuery = true)
    List<Object[]> getOnTimeVsLateDeliveriesWithPercentage(
            @Param("month") Integer month,
            @Param("year") Integer year
    );

    @Query(value = """
                WITH trip_status AS (
                    SELECT 
                        va.id AS vehicle_assignment_id,
                        d.id AS driver_id,
                        u.full_name,
                        MAX(od.estimated_end_time) AS estimated_end_time,
                        MAX(od.end_time) AS actual_end_time
                    FROM drivers d
                    JOIN vehicle_assignments va 
                        ON d.id = va.driver_id_1 OR d.id = va.driver_id_2
                    JOIN order_details od 
                        ON va.id = od.vehicle_assignment_id
                    JOIN orders o 
                        ON od.order_id = o.id
                    JOIN users u 
                        ON u.id = d.user_id
                    WHERE od.end_time IS NOT NULL
                      AND (:year IS NULL OR EXTRACT(YEAR FROM o.created_at) = :year)
                      AND (:month IS NULL OR EXTRACT(MONTH FROM o.created_at) = :month)
                    GROUP BY va.id, d.id, u.full_name
                )
                SELECT 
                    driver_id,
                    full_name,
                    COUNT(vehicle_assignment_id) AS total_trips,
                    SUM(CASE WHEN actual_end_time <= estimated_end_time THEN 1 ELSE 0 END) AS on_time_trips,
                    ROUND(
                        (SUM(CASE WHEN actual_end_time <= estimated_end_time THEN 1 ELSE 0 END) * 100.0) /
                        NULLIF(COUNT(vehicle_assignment_id), 0),
                        2
                    ) AS on_time_percentage
                FROM trip_status
                GROUP BY driver_id, full_name
                ORDER BY on_time_trips DESC
                LIMIT :amount
            """, nativeQuery = true)
    List<Object[]> topOnTimeDeliveriesByDriversWithPercentage(
            @Param("month") Integer month,
            @Param("year") Integer year,
            @Param("amount") int amount
    );


    @Query(value = """
            WITH trip_status AS (
                SELECT
                    va.id AS vehicle_assignment_id,
                    d.id AS driver_id,
                    u.full_name,
                    MAX(od.estimated_end_time) AS estimated_end_time,
                    MAX(od.end_time) AS actual_end_time
                FROM drivers d
                         JOIN vehicle_assignments va
                              ON d.id = va.driver_id_1 OR d.id = va.driver_id_2
                         JOIN order_details od
                              ON va.id = od.vehicle_assignment_id
                         JOIN orders o
                              ON od.order_id = o.id
                         JOIN users u
                              ON u.id = d.user_id
                WHERE od.end_time IS NOT NULL
                  AND (:year IS NULL OR EXTRACT(YEAR FROM o.created_at) = :year)
                  AND (:month IS NULL OR EXTRACT(MONTH FROM o.created_at) = :month)
                GROUP BY va.id, d.id, u.full_name
            )
            SELECT
                driver_id,
                full_name,
                COUNT(vehicle_assignment_id) AS total_trips,
                SUM(CASE WHEN actual_end_time > estimated_end_time THEN 1 ELSE 0 END) AS late_trips,
                ROUND(
                        (SUM(CASE WHEN actual_end_time > estimated_end_time THEN 1 ELSE 0 END) * 100.0) /
                        NULLIF(COUNT(vehicle_assignment_id), 0),
                        2
                ) AS late_percentage
            FROM trip_status
            GROUP BY driver_id, full_name
            ORDER BY late_trips DESC
            LIMIT :amount;
            """, nativeQuery = true)
    List<Object[]> topLateDeliveriesByDriversWithPercentage(
            @Param("month") Integer month,
            @Param("year") Integer year,
            @Param("amount") int amount
    );

    /**
     * Find order details by vehicle assignment entity
     * @param vehicleAssignment The vehicle assignment entity
     * @return List of order details associated with the vehicle assignment
     */
    List<OrderDetailEntity> findByVehicleAssignmentEntity(VehicleAssignmentEntity vehicleAssignment);
    
    /**
     * Find the first order detail associated with a specific vehicle assignment
     * @param vehicleAssignmentId The ID of the vehicle assignment
     * @return The first order detail found, if any
     */
    Optional<OrderDetailEntity> findFirstByVehicleAssignmentEntityId(UUID vehicleAssignmentId);

    /**
     * Find all order details by vehicle assignment ID
     * @param vehicleAssignmentId The ID of the vehicle assignment
     * @return List of order details associated with the vehicle assignment
     */
    List<OrderDetailEntity> findByVehicleAssignmentEntityId(UUID vehicleAssignmentId);
}
