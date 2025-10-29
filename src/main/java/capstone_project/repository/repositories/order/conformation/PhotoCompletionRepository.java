package capstone_project.repository.repositories.order.conformation;

import capstone_project.entity.order.confirmation.PhotoCompletionEntity;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.repository.repositories.common.BaseRepository;

import java.util.List;

public interface PhotoCompletionRepository extends BaseRepository<PhotoCompletionEntity> {
    List<PhotoCompletionEntity> findByVehicleAssignmentEntity(VehicleAssignmentEntity vehicleAssignmentEntity);
}
