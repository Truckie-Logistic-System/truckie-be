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

        log.debug("Received location update via WebSocket for vehicle: {}, plate: {}, lat: {}, lng: {}",
                vehicleId, message.getLicensePlateNumber(), message.getLatitude(), message.getLongitude());

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
            log.debug("Location unchanged for vehicle: {}, skipping broadcast", vehicleId);
            return;
        }

        // Broadcast directly with provided license plate number to avoid extra DB query
        vehicleLocationService.broadcastVehicleLocation(
                vehicleId,
                message.getLatitude(),
                message.getLongitude(),
                message.getLicensePlateNumber()
        );

        log.info("Successfully updated and broadcast location for vehicle: {} ({})", vehicleId, message.getLicensePlateNumber());
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

        log.info("🔵 [WebSocket] Received rate-limited location update for vehicle: {}", vehicleId);
        log.info("   - License Plate: {}", message.getLicensePlateNumber());
        log.info("   - Latitude: {}", message.getLatitude());
        log.info("   - Longitude: {}", message.getLongitude());
        log.info("   - Bearing: {}", message.getBearing());
        log.info("   - Speed: {}", message.getSpeed());

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
            // Broadcast directly with provided license plate number, bearing, and speed from mobile
            vehicleLocationService.broadcastVehicleLocation(
                    vehicleId,
                    message.getLatitude(),
                    message.getLongitude(),
                    message.getLicensePlateNumber(),
                    message.getBearing(),
                    message.getSpeed()
            );
            log.info("✅ [WebSocket] Successfully updated and broadcast location for vehicle: {} ({}), speed: {}km/h",
                    vehicleId, message.getLicensePlateNumber(), message.getSpeed());
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
