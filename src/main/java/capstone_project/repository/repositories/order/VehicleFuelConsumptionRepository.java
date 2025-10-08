package capstone_project.repository.repositories.order;

import capstone_project.entity.order.order.VehicleFuelConsumptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VehicleFuelConsumptionRepository extends JpaRepository<VehicleFuelConsumptionEntity, UUID> {

    @Query("""
        SELECT vfc
        FROM VehicleFuelConsumptionEntity vfc
        LEFT JOIN FETCH vfc.vehicleAssignmentEntity va
        LEFT JOIN FETCH va.vehicleEntity ve
        LEFT JOIN FETCH ve.vehicleTypeEntity vt
        WHERE vfc.vehicleAssignmentEntity.id = :vehicleAssignmentId
        """)
    Optional<VehicleFuelConsumptionEntity> findByVehicleAssignmentId(@Param("vehicleAssignmentId") UUID vehicleAssignmentId);
}
