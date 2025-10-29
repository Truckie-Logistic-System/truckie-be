package capstone_project.repository.repositories.order.conformation;

import capstone_project.entity.order.confirmation.PackingProofImageEntity;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.repository.repositories.common.BaseRepository;

import java.util.List;

public interface PackingProofImageRepository extends BaseRepository<PackingProofImageEntity> {
    List<PackingProofImageEntity> findByVehicleAssignmentEntity(VehicleAssignmentEntity vehicleAssignmentEntity);
}
