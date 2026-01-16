package capstone_project.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class OrderStatusChangedEvent {
    private final UUID orderId;
    private final String oldStatus;
    private final String newStatus;
    private final UUID changedBy;
    private final LocalDateTime timestamp;
    private final String orderCode;
}
