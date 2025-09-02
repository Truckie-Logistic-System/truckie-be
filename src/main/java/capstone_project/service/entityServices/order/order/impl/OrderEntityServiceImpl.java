package capstone_project.service.entityServices.order.order.impl;

import capstone_project.entity.order.order.OrderEntity;
import capstone_project.repository.order.order.OrderRepository;
import capstone_project.service.entityServices.order.order.OrderEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderEntityServiceImpl implements OrderEntityService {

    private final OrderRepository orderRepository;

    @Override
    public OrderEntity save(OrderEntity entity) {
        return orderRepository.save(entity);
    }

    @Override
    public Optional<OrderEntity> findEntityById(UUID uuid) {
        return orderRepository.findById(uuid);
    }

    @Override
    public List<OrderEntity> findAll() {
        return orderRepository.findAll();
    }

    @Override
    public List<OrderEntity> findBySenderId(UUID senderId) {
        return orderRepository.findBySenderId(senderId);
    }

    @Override
    public List<OrderEntity> findByDeliveryAddressId(UUID deliveryAddressId) {
        return orderRepository.findByDeliveryAddressId(deliveryAddressId);
    }
}
