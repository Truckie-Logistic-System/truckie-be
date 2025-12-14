package capstone_project.repository.repositories.vehicle;

import capstone_project.entity.order.order.OrderDetailEntity;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.entity.vehicle.VehicleEntity;
import capstone_project.repository.repositories.common.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VehicleAssignmentRepository extends BaseRepository<VehicleAssignmentEntity> {
    List<VehicleAssignmentEntity> findByStatus(String status);

    List<VehicleAssignmentEntity> findByVehicleEntityId(UUID vehicleEntityId);

    // Add missing method for finding by vehicle ID and status list
    List<VehicleAssignmentEntity> findByVehicleEntityIdAndStatusIn(UUID vehicleId, List<String> statuses);

    /**
     * Find active vehicle assignments with their orders for real-time tracking
     * Only return assignments that have active order details
     */
    @Query(value = """
            SELECT DISTINCT va.*, od.order_id as order_id
            FROM vehicle_assignments va
            JOIN order_details od ON od.vehicle_assignment_id = va.id
            WHERE va.vehicle_id = :vehicleId
            AND od.status IN :orderStatuses
            """, nativeQuery = true)
    List<VehicleAssignmentEntity> findActiveAssignmentsWithOrdersByVehicleId(
            @Param("vehicleId") UUID vehicleId,
            @Param("orderStatuses") List<String> orderStatuses);

    @Query(value = """
        SELECT va.id,
               va.driver_id_1,
               va.driver_id_2,
               v.id,
               COUNT(od.vehicle_assignment_id) AS journey_completed
        FROM vehicle_assignments va
        JOIN vehicles v ON v.id = va.vehicle_id
        JOIN vehicle_types vt ON vt.id = v.vehicle_type_id
        LEFT JOIN order_details od ON od.vehicle_assignment_id = va.id
        WHERE va.status = 'INACTIVE'
          AND vt.id = :vehicleTypeId
          AND va.id NOT IN (
              SELECT va.id
              FROM vehicle_assignments va
              JOIN vehicles v ON v.id = va.vehicle_id
              JOIN vehicle_types vt ON vt.id = v.vehicle_type_id
              JOIN order_details od ON od.vehicle_assignment_id = va.id
              WHERE od.estimated_start_time >= CURRENT_DATE
                AND od.estimated_start_time < CURRENT_DATE + INTERVAL '1 day'
              GROUP BY v.id, va.id
          )
        GROUP BY va.id, va.driver_id_1, va.driver_id_2, v.id
        ORDER BY journey_completed ASC
        """, nativeQuery = true)
    List<VehicleAssignmentEntity> findAssignmentsOrderByActiveCountAscAndVehicleType(@Param("vehicleTypeId") UUID vehicleTypeId);

    @Query(
            value = """
            SELECT va.* 
            FROM vehicle_assignments va
            JOIN order_details od ON od.vehicle_assignment_id = va.id 
            JOIN orders o ON o.id = od.order_id 
            WHERE o.id = :orderId
            """,
            nativeQuery = true
    )
    List<VehicleAssignmentEntity> findVehicleAssignmentsWithOrderID(@Param("orderId") UUID orderID);

    /**
     * Optimized query for WebSocket tracking - eagerly loads all required relationships
     * to prevent LazyInitializationException
     * Loads: vehicle + vehicleType, driver1 + user, driver2 + user, devices + deviceType
     */
    @Query("""
            SELECT DISTINCT va 
            FROM VehicleAssignmentEntity va
            LEFT JOIN FETCH va.vehicleEntity v
            LEFT JOIN FETCH v.vehicleTypeEntity vt
            LEFT JOIN FETCH vt.fuelTypeEntity ft
            LEFT JOIN FETCH va.driver1 d1
            LEFT JOIN FETCH d1.user u1
            LEFT JOIN FETCH va.driver2 d2
            LEFT JOIN FETCH d2.user u2
            LEFT JOIN FETCH va.vehicleAssignmentDevices vad
            LEFT JOIN FETCH vad.device d
            LEFT JOIN FETCH d.deviceTypeEntity dt
            LEFT JOIN FETCH d.vehicleEntity dv
            LEFT JOIN FETCH dv.vehicleTypeEntity dvt
            LEFT JOIN FETCH dvt.fuelTypeEntity dvft
            WHERE va.id IN (
                SELECT va2.id 
                FROM VehicleAssignmentEntity va2
                JOIN OrderDetailEntity od ON od.vehicleAssignmentEntity.id = va2.id
                WHERE od.orderEntity.id = :orderId
            )
            """)
    List<VehicleAssignmentEntity> findVehicleAssignmentsWithOrderIDOptimized(@Param("orderId") UUID orderID);

    Optional<VehicleAssignmentEntity> findVehicleAssignmentByVehicleEntityAndStatus(VehicleEntity vehicleId, String status);

    @Query("""
    SELECT va
    FROM VehicleAssignmentEntity va
    WHERE va.vehicleEntity = :vehicle
    ORDER BY va.createdAt DESC
""")
    List<VehicleAssignmentEntity> findAssignmentsByVehicleOrderByCreatedAtDesc(@Param("vehicle") VehicleEntity vehicle);

    @Query("SELECT va.vehicleEntity.id, COUNT(va) " +
            "FROM VehicleAssignmentEntity va " +
            "WHERE va.createdAt >= :startOfMonth AND va.createdAt < :endOfMonth " +
            "AND va.vehicleEntity.id IN :vehicleIds " +
            "GROUP BY va.vehicleEntity.id")
    List<Object[]> countAssignmentsThisMonthForVehicles(
            @Param("vehicleIds") List<UUID> vehicleIds,
            @Param("startOfMonth") LocalDateTime startOfMonth,
            @Param("endOfMonth") LocalDateTime endOfMonth
    );

    boolean existsByDriver1IdAndStatus(UUID driverId, String status);
    boolean existsByDriver2IdAndStatus(UUID driverId, String status);

    /**
     * Đếm số chuyến đã hoàn thành mà tài xế là driver1
     * @param driverId ID của tài xế
     * @return Số chuyến đã hoàn thành
     */
    @Query(value = """
        SELECT COUNT(va.id)
        FROM vehicle_assignments va
        JOIN order_details od ON od.vehicle_assignment_id = va.id
        WHERE va.driver_id_1 = :driverId
        AND od.status = 'COMPLETED'
    """, nativeQuery = true)
    int countCompletedTripsAsDriver1(@Param("driverId") UUID driverId);

    /**
     * Đếm số chuyến đã hoàn thành mà tài xế là driver2
     * @param driverId ID của tài xế
     * @return Số chuyến đã hoàn thành
     */
    @Query(value = """
        SELECT COUNT(va.id)
        FROM vehicle_assignments va
        JOIN order_details od ON od.vehicle_assignment_id = va.id
        WHERE va.driver_id_2 = :driverId
        AND od.status = 'COMPLETED'
    """, nativeQuery = true)
    int countCompletedTripsAsDriver2(@Param("driverId") UUID driverId);

    /**
     * Tìm vehicle assignment gần nhất của tài xế (driver1)
     */
    @Query("SELECT va FROM VehicleAssignmentEntity va WHERE va.driver1.id = :driverId ORDER BY va.createdAt DESC")
    Optional<VehicleAssignmentEntity> findLatestAssignmentByDriver1Id(@Param("driverId") UUID driverId);

    /**
     * Tìm vehicle assignment gần nhất của tài xế (driver2)
     */
    @Query("SELECT va FROM VehicleAssignmentEntity va WHERE va.driver2.id = :driverId ORDER BY va.createdAt DESC")
    Optional<VehicleAssignmentEntity> findLatestAssignmentByDriver2Id(@Param("driverId") UUID driverId);

    /**
     * Find vehicle assignment with eagerly fetched driver and device relationships
     */
    @Query("SELECT DISTINCT va FROM VehicleAssignmentEntity va " +
           "LEFT JOIN FETCH va.driver1 d1 " +
           "LEFT JOIN FETCH d1.user u1 " +
           "LEFT JOIN FETCH va.driver2 d2 " +
           "LEFT JOIN FETCH d2.user u2 " +
           "WHERE va.id = :assignmentId")
    Optional<VehicleAssignmentEntity> findByIdWithDriversAndDevices(@Param("assignmentId") UUID assignmentId);

    /**
     * Find vehicle assignment with eagerly fetched driver relationships
     */
    @Query("SELECT va FROM VehicleAssignmentEntity va " +
           "LEFT JOIN FETCH va.driver1 d1 " +
           "LEFT JOIN FETCH d1.user u1 " +
           "LEFT JOIN FETCH va.driver2 d2 " +
           "LEFT JOIN FETCH d2.user u2 " +
           "WHERE va.id = :assignmentId")
    Optional<VehicleAssignmentEntity> findByIdWithDrivers(@Param("assignmentId") UUID assignmentId);

    // Removed findByIdWithDevices - use native SQL query in VehicleAssignmentDeviceRepository instead

    /**
     * Find all vehicle assignments by vehicle ID with eagerly fetched driver relationships
     */
    @Query("SELECT va FROM VehicleAssignmentEntity va " +
           "LEFT JOIN FETCH va.driver1 d1 " +
           "LEFT JOIN FETCH d1.user u1 " +
           "LEFT JOIN FETCH va.driver2 d2 " +
           "LEFT JOIN FETCH d2.user u2 " +
           "WHERE va.vehicleEntity.id = :vehicleId")
    List<VehicleAssignmentEntity> findByVehicleEntityIdWithDrivers(@Param("vehicleId") UUID vehicleId);

    /**
     * Tìm tất cả các assignment của tài xế (cả driver1 và driver2) kể từ một thời điểm cụ thể
     * @param driverId ID của tài xế
     * @param cutoffDate Ngày bắt đầu tìm kiếm
     * @return Danh sách các assignment của tài xế từ cutoffDate đến hiện tại
     */
    @Query("SELECT va FROM VehicleAssignmentEntity va " +
           "WHERE (va.driver1.id = :driverId OR va.driver2.id = :driverId) " +
           "AND va.createdAt >= :cutoffDate " +
           "ORDER BY va.createdAt DESC")
    List<VehicleAssignmentEntity> findAssignmentsForDriverSince(
            @Param("driverId") UUID driverId,
            @Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Find all vehicle assignments by driver (primary or secondary), ordered by creation date desc
     */
    @Query("SELECT va FROM VehicleAssignmentEntity va " +
           "LEFT JOIN FETCH va.vehicleEntity v " +
           "LEFT JOIN FETCH va.driver1 d1 " +
           "LEFT JOIN FETCH d1.user u1 " +
           "WHERE va.driver1.id = :driverId OR va.driver2.id = :driverId " +
           "ORDER BY va.createdAt DESC NULLS LAST")
    List<VehicleAssignmentEntity> findByPrimaryDriverIdOrSecondaryDriverIdOrderByCreatedAtDesc(
            @Param("driverId") UUID primaryDriverId,
            @Param("driverId") UUID secondaryDriverId);

    /**
     * Find active vehicle assignments for driver1, ordered by creation date desc
     */
    List<VehicleAssignmentEntity> findByDriver1IdAndStatusOrderByCreatedAtDesc(UUID driverId, String status);

    /**
     * Check if a driver has any vehicle assignment on a specific date (B3 - Hard constraint: 1 driver per trip per day)
     * This checks if the driver (as driver1 or driver2) has any assignment where the trip date matches the given date.
     * Trip date is determined from the first OrderDetail's estimatedStartTime in the assignment.
     * 
     * @param driverId ID của tài xế
     * @param tripDate Ngày chuyến (LocalDate)
     * @return true nếu tài xế đã có assignment trong ngày đó, false nếu không
     */
    @Query(value = """
        SELECT EXISTS (
            SELECT 1
            FROM vehicle_assignments va
            JOIN order_details od ON od.vehicle_assignment_id = va.id
            WHERE (va.driver_id_1 = :driverId OR va.driver_id_2 = :driverId)
            AND CAST(od.estimated_start_time AS DATE) = :tripDate
        )
    """, nativeQuery = true)
    boolean existsAssignmentForDriverOnDate(@Param("driverId") UUID driverId, @Param("tripDate") LocalDate tripDate);

    /**
     * Count completed trips by driver and date range for admin dashboard
     */
    @Query("SELECT COUNT(va) FROM VehicleAssignmentEntity va WHERE (va.driver1.id = :driverId OR va.driver2.id = :driverId) AND va.status = :status AND va.modifiedAt BETWEEN :startDate AND :endDate")
    Long countByDriverIdAndStatusAndCreatedAtBetween(@Param("driverId") UUID driverId, @Param("status") String status, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Count total assignments for a vehicle (for load balancing)
     */
    long countByVehicleEntityId(UUID vehicleId);

    /**
     * Find all vehicle assignments created between start and end date
     */
    List<VehicleAssignmentEntity> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Find vehicle assignment by tracking code
     * @param trackingCode the tracking code to search for
     * @return Optional containing the vehicle assignment if found, or empty if not found
     */
    Optional<VehicleAssignmentEntity> findByTrackingCode(String trackingCode);
    
    /**
     * Get top N drivers for a vehicle (as driver1) ordered by trip count
     */
    @Query("""
        SELECT va.driver1.id, va.driver1.user.fullName, va.driver1.user.phoneNumber, va.driver1.status, COUNT(va.id) as tripCount
        FROM VehicleAssignmentEntity va
        WHERE va.vehicleEntity.id = :vehicleId
        AND va.driver1 IS NOT NULL
        GROUP BY va.driver1.id, va.driver1.user.fullName, va.driver1.user.phoneNumber, va.driver1.status
        ORDER BY tripCount DESC
        """)
    List<Object[]> findTopDriversForVehicle(@Param("vehicleId") UUID vehicleId);

    /**
     * Find all vehicle assignments that use a specific device using native SQL
     */
    @Query(value = """
        SELECT va.* FROM vehicle_assignments va
        JOIN vehicle_assignment_devices vad ON vad.vehicle_assignment_id = va.id
        WHERE vad.device_id = :deviceId
        ORDER BY va.created_at DESC
        """, nativeQuery = true)
    List<VehicleAssignmentEntity> findByDeviceId(@Param("deviceId") UUID deviceId);

    /**
     * Find active vehicle assignments for a specific vehicle (devices fetched separately)
     */
    @Query("""
        SELECT va FROM VehicleAssignmentEntity va
        WHERE va.vehicleEntity.id = :vehicleId
        AND va.status IN :statuses
        ORDER BY va.createdAt DESC
        """)
    List<VehicleAssignmentEntity> findByVehicleIdAndStatusWithDevices(
        @Param("vehicleId") UUID vehicleId, 
        @Param("statuses") List<String> statuses
    );
}
