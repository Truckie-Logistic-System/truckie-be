package capstone_project.repository.entityServices.order.order.impl;

import capstone_project.entity.order.order.OrderDetailEntity;
import capstone_project.repository.repositories.order.order.OrderDetailRepository;
import capstone_project.repository.entityServices.order.order.OrderDetailEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderDetailEntityServiceImpl implements OrderDetailEntityService {

    private final OrderDetailRepository orderDetailRepository;

    @Override
    public OrderDetailEntity save(OrderDetailEntity entity) {
        return orderDetailRepository.save(entity);
    }

    @Override
    public Optional<OrderDetailEntity> findEntityById(UUID uuid) {
        return orderDetailRepository.findById(uuid);
    }

    @Override
    public List<OrderDetailEntity> findAll() {
        return orderDetailRepository.findAll();
    }

    @Override
    public List<OrderDetailEntity> findOrderDetailEntitiesByOrderEntityId(UUID orderDetailEntityId) {
        return orderDetailRepository.findOrderDetailEntitiesByOrderEntityId(orderDetailEntityId);
    }

    @Override
    @Transactional
    public List<OrderDetailEntity> saveAllOrderDetailEntities(List<OrderDetailEntity> orderDetailEntities) {
        List<OrderDetailEntity> savedEntities = orderDetailRepository.saveAll(orderDetailEntities);
        if(savedEntities.size() == orderDetailEntities.size()){
            return savedEntities;
        }
        return null;
    }

    @Override
    public List<OrderDetailEntity> findAllByIds(List<UUID> ids) {
        return orderDetailRepository.findAllById(ids);
    }


}
