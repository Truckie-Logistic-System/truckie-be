package capstone_project.repository.repositories.vehicle;

import capstone_project.entity.vehicle.VehicleEntity;
import capstone_project.entity.vehicle.VehicleTypeEntity;
import capstone_project.repository.repositories.common.BaseRepository;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface    VehicleRepository extends BaseRepository<VehicleEntity> {
    Optional<VehicleEntity> findByLicensePlateNumber(String licensePlateNumber);

    @Query(value = """
        SELECT v.*, va.*,vt.*,vm.*
        FROM vehicles v
        LEFT JOIN vehicle_assignments va ON v.id = va.vehicle_id
        JOIN vehicle_types vt ON v.vehicle_type_id = vt.id
        LEFT JOIN vehicle_maintenance vm ON v.id = vm.vehicle_id
        WHERE v.id = :id
        """, nativeQuery = true)
    Optional<VehicleEntity> findVehicleWithJoinsById(@Param("id") UUID id);

    List<VehicleEntity> getVehicleEntitiesByVehicleTypeEntity(VehicleTypeEntity vehicleTypeEntity);

    List<VehicleEntity> getVehicleEntitiesByVehicleTypeEntityAndStatus(VehicleTypeEntity vehicleTypeEntity, String status);

}