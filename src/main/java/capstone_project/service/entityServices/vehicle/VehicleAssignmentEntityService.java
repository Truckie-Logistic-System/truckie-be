package capstone_project.service.entityServices.vehicle;

import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.service.entityServices.common.BaseEntityService;

import java.util.List;
import java.util.UUID;

public interface VehicleAssignmentEntityService extends BaseEntityService<VehicleAssignmentEntity, UUID> {
    List<VehicleAssignmentEntity> findByStatus(String status);
}
