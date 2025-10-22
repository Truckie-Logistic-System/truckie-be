package capstone_project.repository.entityServices.order.order.impl;

import capstone_project.entity.order.order.OrderSealEntity;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.repository.entityServices.order.order.OrderSealEntityService;
import capstone_project.repository.repositories.order.order.OrderSealRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderSealEntityServiceImpl implements OrderSealEntityService {
    private final OrderSealRepository orderSealRepository;

    @Override
    public OrderSealEntity save(OrderSealEntity entity) {
        return orderSealRepository.save(entity);
    }

    @Override
    public Optional<OrderSealEntity> findEntityById(UUID uuid) {
        return orderSealRepository.findById(uuid);
    }

    @Override
    public List<OrderSealEntity> findAll() {
        return orderSealRepository.findAll();
    }

    @Override
    public List<OrderSealEntity> saveAll(List<OrderSealEntity> orderSealEntities) {
        return orderSealRepository.saveAll(orderSealEntities);
    }

    @Override
    public List<OrderSealEntity> findBySealCode(String sealCode) {
        return orderSealRepository.findBySealCode(sealCode);
    }

    @Override
    public OrderSealEntity findByVehicleAssignment(VehicleAssignmentEntity vehicleAssignment, String status) {
        return orderSealRepository.findFirstByVehicleAssignmentAndStatus(vehicleAssignment, status);
    }

    @Override
    public List<OrderSealEntity> findAllByVehicleAssignment(VehicleAssignmentEntity vehicleAssignment) {
        return orderSealRepository.findByVehicleAssignment(vehicleAssignment);
    }

    @Override
    public List<OrderSealEntity> findAllByVehicleAssignmentAndStatus(VehicleAssignmentEntity vehicleAssignment, String status) {
        return orderSealRepository.findAllByVehicleAssignmentAndStatus(vehicleAssignment, status);
    }
}
