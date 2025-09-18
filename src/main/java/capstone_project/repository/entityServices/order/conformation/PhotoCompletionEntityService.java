package capstone_project.repository.entityServices.order.conformation;

import capstone_project.entity.order.conformation.PhotoCompletionEntity;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.repository.entityServices.common.BaseEntityService;

import java.util.List;
import java.util.UUID;

public interface PhotoCompletionEntityService extends BaseEntityService<PhotoCompletionEntity, UUID> {
    List<PhotoCompletionEntity> findByVehicleAssignmentEntity(VehicleAssignmentEntity vehicleAssignmentEntity);
}
