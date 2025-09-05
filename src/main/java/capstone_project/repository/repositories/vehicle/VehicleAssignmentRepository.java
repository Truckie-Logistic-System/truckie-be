package capstone_project.repository.repositories.vehicle;

import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.repository.repositories.common.BaseRepository;

import java.util.List;

public interface VehicleAssignmentRepository extends BaseRepository<VehicleAssignmentEntity> {
    // Additional methods specific to VehicleAssignmentEntity can be defined here if needed
    List<VehicleAssignmentEntity> findByStatus(String status);
}
