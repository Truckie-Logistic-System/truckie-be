package capstone_project.repository.repositories.order.order;

import capstone_project.entity.order.order.OrderDetailEntity;
import capstone_project.entity.order.order.OrderDetailSealEntity;
import capstone_project.entity.order.order.SealEntity;
import capstone_project.repository.repositories.common.BaseRepository;

import java.util.List;

public interface OrderDetailSealRepository extends BaseRepository<OrderDetailSealEntity> {
    List<OrderDetailSealEntity> findBySeal(SealEntity seal);

    OrderDetailSealEntity findByOrderDetailAndStatus(OrderDetailEntity orderDetail, String status);
}
