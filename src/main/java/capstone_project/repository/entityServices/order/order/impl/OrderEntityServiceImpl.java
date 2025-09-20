package capstone_project.repository.entityServices.order.order.impl;

import capstone_project.entity.order.order.OrderEntity;
import capstone_project.repository.repositories.order.order.OrderRepository;
import capstone_project.repository.entityServices.order.order.OrderEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
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
        return orderRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Override
    public List<OrderEntity> findBySenderId(UUID senderId) {
        return orderRepository.findBySenderIdOrderByCreatedAtDesc(senderId);
    }

    @Override
    public List<OrderEntity> findByDeliveryAddressId(UUID deliveryAddressId) {
        return orderRepository.findByDeliveryAddressId(deliveryAddressId);
    }

    @Override
    public List<OrderEntity> findRecentOrdersByCustomerId(UUID customerId, int limit) {
        return orderRepository.findRecentOrdersByCustomerId(customerId, limit);
    }
}
