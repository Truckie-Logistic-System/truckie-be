package capstone_project.repository.repositories.vehicle;

import capstone_project.entity.vehicle.VehicleEntity;
import capstone_project.repository.repositories.common.BaseRepository;

import java.util.Optional;

public interface VehicleRepository extends BaseRepository<VehicleEntity> {
    Optional<VehicleEntity> findByLicensePlateNumber(String licensePlateNumber);

}
