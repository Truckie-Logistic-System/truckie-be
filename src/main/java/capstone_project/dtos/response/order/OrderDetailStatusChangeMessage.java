package capstone_project.dtos.response.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for WebSocket order detail status change notifications
 * Sent to frontend when individual order detail (package) status changes
 * This allows real-time tracking of each package in an order
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailStatusChangeMessage {
    /**
     * Order Detail ID
     */
    private UUID orderDetailId;
    
    /**
     * Tracking code of the order detail (e.g., "ORD_D_20251104210008-9681")
     */
    private String trackingCode;
    
    /**
     * Parent Order ID (for routing WebSocket message)
     */
    private UUID orderId;
    
    /**
     * Parent Order Code (for display)
     */
    private String orderCode;
    
    /**
     * Vehicle Assignment ID (to identify which trip this package belongs to)
     * Null if not yet assigned to a vehicle
     */
    private UUID vehicleAssignmentId;
    
    /**
     * Previous order detail status
     */
    private String previousStatus;
    
    /**
     * New order detail status
     */
    private String newStatus;
    
    /**
     * Timestamp when status changed
     */
    private Instant timestamp;
    
    /**
     * Human-readable message for the status change
     */
    private String message;
}
