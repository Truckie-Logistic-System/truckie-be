package capstone_project.event.listener;

import capstone_project.event.OrderAssignedEvent;
import capstone_project.event.OrderCreatedEvent;
import capstone_project.event.OrderStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventListener {

    @Async
    @EventListener
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Order created event received. OrderId: {}, OrderCode: {}, CustomerId: {}, Quantity: {}", 
            event.getOrderId(), 
            event.getOrderCode(), 
            event.getCustomerId(), 
            event.getTotalQuantity());
        
        try {
            // Business logic for order creation
            // - Send confirmation email/notification to customer
            // - Update analytics/metrics
            // - Trigger inventory checks
            log.debug("Processing order creation side effects for order: {}", event.getOrderCode());
            
        } catch (Exception ex) {
            log.error("Error handling order created event for order: {}", event.getOrderCode(), ex);
        }
    }

    @Async
    @EventListener
    public void handleOrderStatusChanged(OrderStatusChangedEvent event) {
        log.info("Order status changed event received. OrderId: {}, OrderCode: {}, OldStatus: {}, NewStatus: {}", 
            event.getOrderId(), 
            event.getOrderCode(),
            event.getOldStatus(), 
            event.getNewStatus());
        
        try {
            // Business logic for status change
            // - Send status update notification to customer
            // - Update driver dashboard via WebSocket
            // - Log audit trail
            // - Trigger workflow based on new status
            
            if ("COMPLETED".equals(event.getNewStatus())) {
                log.info("Order completed. Triggering completion workflows for order: {}", event.getOrderCode());
                // - Calculate driver earnings
                // - Update customer order history
                // - Send completion notification
            } else if ("CANCELLED".equals(event.getNewStatus())) {
                log.info("Order cancelled. Triggering cancellation workflows for order: {}", event.getOrderCode());
                // - Release assigned driver/vehicle
                // - Process refund if applicable
                // - Update availability
            }
            
        } catch (Exception ex) {
            log.error("Error handling order status changed event for order: {}", event.getOrderCode(), ex);
        }
    }

    @Async
    @EventListener
    public void handleOrderAssigned(OrderAssignedEvent event) {
        log.info("Order assigned event received. OrderId: {}, OrderCode: {}, DriverId: {}, VehicleId: {}", 
            event.getOrderId(), 
            event.getOrderCode(),
            event.getDriverId(), 
            event.getVehicleId());
        
        try {
            // Business logic for order assignment
            // - Send assignment notification to driver
            // - Update vehicle status to "assigned"
            // - Send customer notification with driver details
            // - Start tracking/monitoring
            log.debug("Processing order assignment side effects for order: {}", event.getOrderCode());
            
        } catch (Exception ex) {
            log.error("Error handling order assigned event for order: {}", event.getOrderCode(), ex);
        }
    }
}
