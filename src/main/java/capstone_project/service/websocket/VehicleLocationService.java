package capstone_project.service.websocket;

import capstone_project.dtos.websocket.VehicleLocationMessage;
import capstone_project.entity.vehicle.VehicleEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VehicleLocationService {

    private final SimpMessagingTemplate messagingTemplate;

    private static final String TOPIC_ALL_VEHICLES = "/topic/vehicles/locations";
    private static final String TOPIC_VEHICLE_PREFIX = "/topic/vehicles/";

    /**
     * Broadcast updated vehicle location to all subscribers
     */
    public void broadcastVehicleLocation(VehicleEntity vehicle) {
        if (vehicle == null || vehicle.getCurrentLatitude() == null || vehicle.getCurrentLongitude() == null) {
            return;
        }

        VehicleLocationMessage message = VehicleLocationMessage.builder()
                .vehicleId(vehicle.getId())
                .latitude(vehicle.getCurrentLatitude())
                .longitude(vehicle.getCurrentLongitude())
                .licensePlateNumber(vehicle.getLicensePlateNumber())
                .build();

        broadcastVehicleLocation(message);
    }

    /**
     * Broadcast updated vehicle location to all subscribers using the message DTO
     */
    public void broadcastVehicleLocation(VehicleLocationMessage message) {
        // Broadcast to all vehicles topic for web clients
        messagingTemplate.convertAndSend(TOPIC_ALL_VEHICLES, message);

        // Broadcast to specific vehicle topic for clients tracking specific vehicle
        messagingTemplate.convertAndSend(TOPIC_VEHICLE_PREFIX + message.getVehicleId(), message);

        log.debug("Broadcast vehicle location: vehicleId={}, lat={}, lng={}",
                message.getVehicleId(), message.getLatitude(), message.getLongitude());
    }

    /**
     * Broadcast vehicle location update with basic parameters
     */
    public void broadcastVehicleLocation(UUID vehicleId, BigDecimal latitude, BigDecimal longitude, String licensePlateNumber) {
        VehicleLocationMessage message = VehicleLocationMessage.builder()
                .vehicleId(vehicleId)
                .latitude(latitude)
                .longitude(longitude)
                .licensePlateNumber(licensePlateNumber)
                .build();

        broadcastVehicleLocation(message);
    }
}
