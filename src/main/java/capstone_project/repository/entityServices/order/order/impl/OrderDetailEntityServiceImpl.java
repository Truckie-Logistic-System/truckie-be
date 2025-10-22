package capstone_project.repository.entityServices.order.order.impl;

import capstone_project.entity.order.order.OrderDetailEntity;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.repository.repositories.order.order.OrderDetailRepository;
import capstone_project.repository.entityServices.order.order.OrderDetailEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
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
        return orderDetailRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Override
    public List<OrderDetailEntity> findOrderDetailEntitiesByOrderEntityId(UUID orderDetailEntityId) {
        return orderDetailRepository.findOrderDetailEntitiesByOrderEntityIdOrderByCreatedAtDesc(orderDetailEntityId);
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

    @Override
    public Optional<OrderDetailEntity> findByTrackingCode(String trackingCode) {
        return orderDetailRepository.findByTrackingCode(trackingCode);
    }

    @Override
    public List<Object[]> getOnTimeVsLateDeliveriesWithPercentage(Integer month, Integer year) {
        return orderDetailRepository.getOnTimeVsLateDeliveriesWithPercentage(month, year);
    }

    @Override
    public List<Object[]> topOnTimeDeliveriesByDriversWithPercentage(Integer month, Integer year, int amount) {
        return orderDetailRepository.topOnTimeDeliveriesByDriversWithPercentage(month, year, amount);
    }

    @Override
    public List<Object[]> topLateDeliveriesByDriversWithPercentage(Integer month, Integer year, int amount) {
        return orderDetailRepository.topLateDeliveriesByDriversWithPercentage(month, year, amount);
    }

    @Override
    public List<OrderDetailEntity> findByVehicleAssignmentEntity(VehicleAssignmentEntity vehicleAssignment) {
        return orderDetailRepository.findByVehicleAssignmentEntity(vehicleAssignment);
    }
}
