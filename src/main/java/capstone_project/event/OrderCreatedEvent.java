package capstone_project.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class OrderCreatedEvent {
    private final UUID orderId;
    private final String orderCode;
    private final UUID customerId;
    private final Integer totalQuantity;
    private final LocalDateTime createdAt;
}
