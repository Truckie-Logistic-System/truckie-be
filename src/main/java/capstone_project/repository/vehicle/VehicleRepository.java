package capstone_project.repository.vehicle;

import capstone_project.entity.vehicle.VehicleEntity;
import capstone_project.repository.common.BaseRepository;

import java.util.Optional;
import java.util.UUID;

public interface VehicleRepository extends BaseRepository<VehicleEntity> {
    Optional<VehicleEntity> findByLicensePlateNumber(String licensePlateNumber);

}
