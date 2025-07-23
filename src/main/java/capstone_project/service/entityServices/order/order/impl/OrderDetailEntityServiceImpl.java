package capstone_project.service.entityServices.order.order.impl;

import capstone_project.entity.order.order.OrderDetailEntity;
import capstone_project.repository.order.order.OrderDetailRepository;
import capstone_project.service.entityServices.order.order.OrderDetailEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
    public Optional<OrderDetailEntity> findById(UUID uuid) {
        return orderDetailRepository.findById(uuid);
    }

    @Override
    public List<OrderDetailEntity> findAll() {
        return orderDetailRepository.findAll();
    }
}
