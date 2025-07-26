package capstone_project.repository.vehicle;

import capstone_project.entity.vehicle.VehicleTypeEntity;
import capstone_project.repository.common.BaseRepository;

import java.util.Optional;

public interface VehicleTypeRepository extends BaseRepository<VehicleTypeEntity> {

    Optional<VehicleTypeEntity> findByVehicleTypeName(String vehicleTypeName);
}
