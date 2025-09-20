package capstone_project.dtos.response.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceiverDetailResponse {
    private String receiverName;
    private String receiverPhone;
    private String receiverIdentity;
    private UUID pickupAddressId;
    private UUID deliveryAddressId;
    private AddressResponse pickupAddress;
    private AddressResponse deliveryAddress;
}
