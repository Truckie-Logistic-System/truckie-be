package capstone_project.repository.entityServices.order.order;

import capstone_project.entity.order.order.OrderDetailEntity;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.repository.entityServices.common.BaseEntityService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderDetailEntityService extends BaseEntityService<OrderDetailEntity, UUID> {
    List<OrderDetailEntity> findOrderDetailEntitiesByOrderEntityId(UUID orderDetailEntityId);

    List<OrderDetailEntity> saveAllOrderDetailEntities(List<OrderDetailEntity> orderDetailEntities);

    List<OrderDetailEntity> findAllByIds(List<UUID> ids);

    /**
     * Find an order detail by its tracking code
     * @param trackingCode The tracking code to search for
     * @return Optional containing the OrderDetailEntity if found, or empty Optional otherwise
     */
    Optional<OrderDetailEntity> findByTrackingCode(String trackingCode);

    List<Object[]> getOnTimeVsLateDeliveriesWithPercentage(Integer month, Integer year);

    List<Object[]> topOnTimeDeliveriesByDriversWithPercentage(Integer month, Integer year, int amount);

    List<Object[]> topLateDeliveriesByDriversWithPercentage(Integer month, Integer year, int amount);

    /**
     * Find order details by vehicle assignment entity
     * @param vehicleAssignment The vehicle assignment entity
     * @return List of order details associated with the vehicle assignment
     */
    List<OrderDetailEntity> findByVehicleAssignmentEntity(VehicleAssignmentEntity vehicleAssignment);
    
    /**
     * Find order details by vehicle assignment ID
     * @param vehicleAssignmentId The vehicle assignment ID
     * @return List of order details associated with the vehicle assignment
     */
    List<OrderDetailEntity> findByVehicleAssignmentId(UUID vehicleAssignmentId);
}
