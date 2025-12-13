package capstone_project.repository.repositories.vehicle;

import capstone_project.entity.vehicle.VehicleAssignmentDeviceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VehicleAssignmentDeviceRepository extends JpaRepository<VehicleAssignmentDeviceEntity, VehicleAssignmentDeviceEntity.VehicleAssignmentDeviceId> {

    /**
     * Find all device IDs for a vehicle assignment using native SQL
     */
    @Query(value = """
                SELECT d.id
                FROM devices d
                JOIN vehicle_assignment_devices vad ON vad.device_id = d.id
                WHERE vad.vehicle_assignment_id = :vehicleAssignmentId
            """, nativeQuery = true)
    List<UUID> findDeviceIdsByVehicleAssignmentId(@Param("vehicleAssignmentId") UUID vehicleAssignmentId);

    /**
     * Find all VehicleAssignmentDeviceEntity by vehicle assignment ID using native SQL
     */
    @Query(value = """
                SELECT vad.vehicle_assignment_id, vad.device_id, vad.created_at
                FROM vehicle_assignment_devices vad
                WHERE vad.vehicle_assignment_id = :vehicleAssignmentId
            """, nativeQuery = true)
    List<Object[]> findRawDevicesByVehicleAssignmentId(@Param("vehicleAssignmentId") UUID vehicleAssignmentId);

    /**
     * Delete all device associations for a vehicle assignment using native SQL
     */
    @org.springframework.data.jpa.repository.Modifying
    @Query(value = "DELETE FROM vehicle_assignment_devices WHERE vehicle_assignment_id = :vehicleAssignmentId", nativeQuery = true)
    void deleteByVehicleAssignmentIdNative(@Param("vehicleAssignmentId") UUID vehicleAssignmentId);
}
