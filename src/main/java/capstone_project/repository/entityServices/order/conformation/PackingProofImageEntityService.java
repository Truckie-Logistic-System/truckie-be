package capstone_project.repository.entityServices.order.conformation;

import capstone_project.entity.order.confirmation.PackingProofImageEntity;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PackingProofImageEntityService {
    PackingProofImageEntity save(PackingProofImageEntity entity);
    Optional<PackingProofImageEntity> findEntityById(UUID uuid);
    List<PackingProofImageEntity> findAll();
    List<PackingProofImageEntity> findByVehicleAssignmentEntity(VehicleAssignmentEntity vehicleAssignmentEntity);
}
