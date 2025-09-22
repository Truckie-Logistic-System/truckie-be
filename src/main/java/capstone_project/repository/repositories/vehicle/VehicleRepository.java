package capstone_project.repository.repositories.vehicle;

import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.entity.vehicle.VehicleEntity;
import capstone_project.repository.repositories.common.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VehicleAssignmentRepository extends BaseRepository<VehicleAssignmentEntity> {
    List<VehicleAssignmentEntity> findByStatus(String status);

    List<VehicleAssignmentEntity> findByVehicleEntityId(UUID vehicleEntityId);

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
}
