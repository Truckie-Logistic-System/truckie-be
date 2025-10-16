package capstone_project.controller.websocket;

import capstone_project.dtos.websocket.VehicleLocationMessage;
import capstone_project.service.websocket.VehicleLocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.UUID;

/**
 * Controller for handling vehicle tracking WebSocket communications for web clients
 * Web clients ONLY get location updates, they don't update locations
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class VehicleTrackingController {

    private final SimpMessagingTemplate messagingTemplate;
    private final VehicleLocationService vehicleLocationService;

    /**
     * Handler for customer to get current location of a specific vehicle
     * Web client sends to: /app/vehicle/{vehicleId}/get-location
     * Returns current location immediately
     */
    @MessageMapping("/vehicle/{vehicleId}/get-location")
    public void getVehicleLocation(@DestinationVariable String vehicleId) {
        try {
            UUID vehicleUUID = UUID.fromString(vehicleId);
            log.info("Web client requested current location for vehicle: {}", vehicleId);

            // Get enhanced vehicle location with all details through service
            VehicleLocationMessage locationMessage = vehicleLocationService.getEnhancedVehicleLocation(vehicleUUID);

            if (locationMessage != null) {
                // Send to specific vehicle topic
                messagingTemplate.convertAndSend("/topic/vehicles/" + vehicleId, locationMessage);
                log.debug("Sent enhanced location for vehicle {}", vehicleId);
            } else {
                log.warn("Vehicle not found or no location data for vehicle: {}", vehicleId);
            }

        } catch (IllegalArgumentException e) {
            log.error("Invalid vehicle ID format: {}", vehicleId, e);
        } catch (Exception e) {
            log.error("Error getting current location for vehicle {}", vehicleId, e);
        }
    }

    /**
     * Handler for customer to get locations of all vehicles in an order
     * Web client sends to: /app/order/{orderId}/get-locations
     * Returns current locations of all vehicles assigned to the order
     * with detailed information about vehicles, drivers and tracking
     * orderId can be either UUID or order_code
     */
    @MessageMapping("/order/{orderId}/get-locations")
    public void getOrderVehicleLocations(@DestinationVariable String orderId) {
        try {
            log.info("Web client requested locations for all vehicles in order: {}", orderId);

            // Try to parse as UUID first
            try {
                UUID orderUUID = UUID.fromString(orderId);
                // If successful, use UUID path
                vehicleLocationService.sendOrderVehicleLocations(orderUUID);
            } catch (IllegalArgumentException e) {
                // Not a UUID, treat as order code
                log.debug("Input is not a UUID, treating as order code: {}", orderId);
                vehicleLocationService.sendOrderVehicleLocationsByOrderCode(orderId);
            }

        } catch (Exception e) {
            log.error("Error getting vehicle locations for order identifier {}", orderId, e);
        }
    }
}
