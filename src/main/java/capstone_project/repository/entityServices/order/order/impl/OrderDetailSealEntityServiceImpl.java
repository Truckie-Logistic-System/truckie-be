package capstone_project.repository.entityServices.order.order.impl;

import capstone_project.entity.order.order.OrderDetailEntity;
import capstone_project.entity.order.order.OrderDetailSealEntity;
import capstone_project.entity.order.order.SealEntity;
import capstone_project.repository.entityServices.order.order.OrderDetailSealEntityService;
import capstone_project.repository.repositories.order.order.OrderDetailSealRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderDetailSealEntityServiceImpl implements OrderDetailSealEntityService {
    private final OrderDetailSealRepository orderDetailSealRepository;

    @Override
    public OrderDetailSealEntity save(OrderDetailSealEntity entity) {
        return orderDetailSealRepository.save(entity);
    }

    @Override
    public Optional<OrderDetailSealEntity> findEntityById(UUID uuid) {
        return orderDetailSealRepository.findById(uuid);
    }

    @Override
    public List<OrderDetailSealEntity> findAll() {
        return orderDetailSealRepository.findAll();
    }

    @Override
    public List<OrderDetailSealEntity> saveAll(List<OrderDetailSealEntity> orderDetailSealEntities) {
        return orderDetailSealRepository.saveAll(orderDetailSealEntities);
    }

    @Override
    public List<OrderDetailSealEntity> findBySeal(SealEntity seal) {
        return orderDetailSealRepository.findBySeal(seal);
    }

    @Override
    public OrderDetailSealEntity findByOrderDetail(OrderDetailEntity orderDetail,String status) {
        return orderDetailSealRepository.findByOrderDetailAndStatus(orderDetail,status);
    }
}
