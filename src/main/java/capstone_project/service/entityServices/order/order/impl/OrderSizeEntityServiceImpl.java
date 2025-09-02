package capstone_project.service.entityServices.order.order.impl;

import capstone_project.entity.order.order.OrderSizeEntity;
import capstone_project.repository.order.order.OrderSizeRepository;
import capstone_project.service.entityServices.order.order.OrderSizeEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderSizeEntityServiceImpl implements OrderSizeEntityService {
    private final OrderSizeRepository orderSizeRepository;

    @Override
    public OrderSizeEntity save(OrderSizeEntity entity) {
        return orderSizeRepository.save(entity);
    }

    @Override
    public Optional<OrderSizeEntity> findEntityById(UUID uuid) {
        return orderSizeRepository.findById(uuid);
    }

    @Override
    public List<OrderSizeEntity> findAll() {
        return orderSizeRepository.findAll();
    }
}
