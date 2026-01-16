package capstone_project.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class OrderAssignedEvent {
    private final UUID orderId;
    private final String orderCode;
    private final UUID driverId;
    private final UUID vehicleId;
    private final LocalDateTime assignedAt;
}
