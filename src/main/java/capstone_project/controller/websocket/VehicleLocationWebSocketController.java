package capstone_project.controller.websocket;

import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.exceptions.dto.BadRequestException;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.websocket.MobileLocationUpdateMessage;
import capstone_project.entity.vehicle.VehicleEntity;
import capstone_project.repository.entityServices.vehicle.VehicleEntityService;
import capstone_project.service.websocket.VehicleLocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class VehicleLocationWebSocketController {

    private final VehicleEntityService vehicleEntityService;
    private final VehicleLocationService vehicleLocationService;

    /**
     * Mobile app sends location updates to: /app/vehicle/{vehicleId}/location
     * Server broadcasts to: /topic/vehicles/locations and /topic/vehicles/{vehicleId}
     */
    @MessageMapping("/vehicle/{vehicleId}/location")
    @Transactional
    public void updateVehicleLocation(
            @DestinationVariable("vehicleId") UUID vehicleId,
            @Payload MobileLocationUpdateMessage message) {

        // Validate input
        if (message.getLatitude() == null || message.getLongitude() == null) {
            log.warn("Invalid location data received for vehicle {}: lat={}, lng={}",
                    vehicleId, message.getLatitude(), message.getLongitude());
            throw new BadRequestException(
                    "Latitude and longitude cannot be null",
                    ErrorEnum.INVALID.getErrorCode()
            );
        }

        // Update location using existing service method
        boolean updated = vehicleEntityService.updateLocationDirectly(
                vehicleId, message.getLatitude(), message.getLongitude());

        if (!updated) {
            // Check if vehicle exists
            if (!vehicleEntityService.findByVehicleId(vehicleId).isPresent()) {
                log.warn("Vehicle not found: {}", vehicleId);
                throw new NotFoundException(
                        "Vehicle not found with ID: " + vehicleId,
                        ErrorEnum.NOT_FOUND.getErrorCode()
                );
            }
            
            return;
        }

        // Broadcast directly with provided license plate number to avoid extra DB query
        vehicleLocationService.broadcastVehicleLocation(
                vehicleId,
                message.getLatitude(),
                message.getLongitude(),
                message.getLicensePlateNumber()
        );

    }

    /**
     * Mobile app sends location updates with rate limiting to: /app/vehicle/{vehicleId}/location-rate-limited
     */
    @MessageMapping("/vehicle/{vehicleId}/location-rate-limited")
    @Transactional
    @SendToUser("/queue/location-update-result")
    public boolean updateVehicleLocationWithRateLimit(
            @DestinationVariable("vehicleId") UUID vehicleId,
            @Payload MobileLocationUpdateMessage message) {

        // Validate input
        if (message.getLatitude() == null || message.getLongitude() == null) {
            throw new BadRequestException(
                    "Latitude and longitude cannot be null",
                    ErrorEnum.INVALID.getErrorCode()
            );
        }

        // DEMO OPTIMIZATION: Use 1 second rate limit to prevent visual glitches
        // Reduced from 2s to 1s for faster initial location updates
        boolean updated = vehicleEntityService.updateLocationWithRateLimit(
                vehicleId, message.getLatitude(), message.getLongitude(), 1);

        if (updated) {
            // Build full VehicleLocationMessage including assignment for multi-trip off-route
            capstone_project.dtos.websocket.VehicleLocationMessage wsMessage =
                    capstone_project.dtos.websocket.VehicleLocationMessage.builder()
                            .vehicleId(vehicleId)
                            .latitude(message.getLatitude())
                            .longitude(message.getLongitude())
                            .licensePlateNumber(message.getLicensePlateNumber())
                            .bearing(message.getBearing())
                            .speed(message.getSpeed())
                            .vehicleAssignmentId(message.getVehicleAssignmentId())
                            .build();

            vehicleLocationService.broadcastVehicleLocation(wsMessage);

        } else {
            // Check if vehicle exists
            if (!vehicleEntityService.findByVehicleId(vehicleId).isPresent()) {
                throw new NotFoundException(
                        "Vehicle not found with ID: " + vehicleId,
                        ErrorEnum.NOT_FOUND.getErrorCode()
                );
            }
            log.warn("⚠️ [WebSocket] Rate-limited location update SKIPPED for vehicle: {} ({}) - too soon since last update",
                    vehicleId, message.getLicensePlateNumber());
        }

        return updated;
    }
}
