package capstone_project.repository.entityServices.user;
import capstone_project.entity.user.driver.PenaltyHistoryEntity;
import capstone_project.repository.entityServices.common.BaseEntityService;

import java.util.List;
import java.util.UUID;

public interface PenaltyHistoryEntityService
        extends BaseEntityService<PenaltyHistoryEntity, UUID> {

    List<PenaltyHistoryEntity> findByVehicleAssignmentId(UUID vehicleAssignmentId);

    List<PenaltyHistoryEntity> findByDriverId(UUID driverId);
}
