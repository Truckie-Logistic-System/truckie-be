package capstone_project.repository.repositories.vehicle;

import capstone_project.entity.vehicle.VehicleTypeEntity;
import capstone_project.repository.repositories.common.BaseRepository;

import java.util.Optional;

public interface VehicleTypeRepository extends BaseRepository<VehicleTypeEntity> {

    Optional<VehicleTypeEntity> findByVehicleTypeName(String vehicleTypeName);
}
