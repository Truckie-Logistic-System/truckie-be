package capstone_project.repository.entityServices.order.order;

import capstone_project.entity.order.order.OrderSizeEntity;
import capstone_project.entity.order.order.SealEntity;
import capstone_project.repository.entityServices.common.BaseEntityService;

import java.util.UUID;

public interface SealEntityService extends BaseEntityService<SealEntity, UUID> {
    SealEntity findBySealCode(String sealCode);
}
