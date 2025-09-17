package capstone_project.repository.repositories.vehicle;

import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.repository.repositories.common.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface VehicleAssignmentRepository extends BaseRepository<VehicleAssignmentEntity> {
    List<VehicleAssignmentEntity> findByStatus(String status);

    List<VehicleAssignmentEntity> findByVehicleEntityId(UUID vehicleEntityId);

    @Query(value = """
    SELECT va.*
    FROM vehicle_assignments va
    JOIN vehicles v ON v.id = va.vehicle_id
    JOIN vehicle_types vt ON vt.id = v.vehicle_type_id
    JOIN (
        SELECT v.id AS vehicle_id, COUNT(va.id) AS active_count
        FROM vehicles v
        JOIN vehicle_assignments va ON v.id = va.vehicle_id
        WHERE va.status = 'ACTIVE'
        GROUP BY v.id
    ) sub ON va.vehicle_id = sub.vehicle_id
    WHERE va.status = 'ASSIGNED'
      AND vt.id = :vehicleTypeId
    ORDER BY sub.active_count ASC
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


}
