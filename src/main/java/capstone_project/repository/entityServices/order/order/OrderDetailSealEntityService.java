package capstone_project.repository.entityServices.order.order;

import capstone_project.entity.order.order.OrderDetailEntity;
import capstone_project.entity.order.order.OrderDetailSealEntity;
import capstone_project.entity.order.order.SealEntity;
import capstone_project.repository.entityServices.common.BaseEntityService;

import java.util.List;
import java.util.UUID;

public interface OrderDetailSealEntityService extends BaseEntityService<OrderDetailSealEntity, UUID> {
    List<OrderDetailSealEntity> saveAll(List<OrderDetailSealEntity> orderDetailSealEntities);

    List<OrderDetailSealEntity> findBySeal(SealEntity seal);

    OrderDetailSealEntity findByOrderDetail(OrderDetailEntity orderDetail,String status);
}
