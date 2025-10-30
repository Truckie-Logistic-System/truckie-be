package capstone_project.service.services.order.order;

import capstone_project.common.enums.OrderDetailStatusEnum;
import capstone_project.entity.order.order.OrderDetailEntity;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing OrderDetail status updates
 * Handles status transitions for individual order details within vehicle assignments
 */
public interface OrderDetailStatusService {
    
    /**
     * Update status for all OrderDetails associated with a vehicle assignment
     * This is the primary method called by drivers during delivery
     * 
     * @param vehicleAssignmentId ID of the vehicle assignment (trip)
     * @param newStatus New status to set for all order details in this trip
     */
    void updateOrderDetailStatusByAssignment(UUID vehicleAssignmentId, OrderDetailStatusEnum newStatus);
    
    /**
     * Update status for a specific OrderDetail
     * 
     * @param orderDetailId ID of the order detail
     * @param newStatus New status to set
     */
    void updateOrderDetailStatus(UUID orderDetailId, OrderDetailStatusEnum newStatus);
    
    /**
     * Get all OrderDetails associated with a vehicle assignment
     * 
     * @param vehicleAssignmentId ID of the vehicle assignment
     * @return List of order details for this assignment
     */
    List<OrderDetailEntity> getOrderDetailsByAssignment(UUID vehicleAssignmentId);
    
    /**
     * Validate if status transition is allowed
     * 
     * @param currentStatus Current status
     * @param newStatus Desired new status
     * @return true if transition is valid, false otherwise
     */
    boolean isValidStatusTransition(OrderDetailStatusEnum currentStatus, OrderDetailStatusEnum newStatus);
}
