package capstone_project.dtos.response.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceiverSuggestionResponse {
    private UUID orderId;
    private String receiverName;
    private String receiverPhone;
    private String receiverIdentity;
    private String partialAddress;
    private LocalDateTime orderDate;
}
