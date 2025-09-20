package capstone_project.service.mapper.order;

import capstone_project.dtos.response.order.ReceiverDetailResponse;
import capstone_project.entity.order.order.OrderEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReceiverDetailMapper {

    private final AddressMapper addressMapper;

    /**
     * Map an OrderEntity to ReceiverDetailResponse
     *
     * @param entity OrderEntity to map
     * @return ReceiverDetailResponse
     */
    public ReceiverDetailResponse toDto(OrderEntity entity) {
        if (entity == null) {
            return null;
        }

        return ReceiverDetailResponse.builder()
                .receiverName(entity.getReceiverName())
                .receiverPhone(entity.getReceiverPhone())
                .pickupAddressId(entity.getPickupAddress() != null ? entity.getPickupAddress().getId() : null)
                .deliveryAddressId(entity.getDeliveryAddress() != null ? entity.getDeliveryAddress().getId() : null)
                .pickupAddress(entity.getPickupAddress() != null ? addressMapper.toDto(entity.getPickupAddress()) : null)
                .deliveryAddress(entity.getDeliveryAddress() != null ? addressMapper.toDto(entity.getDeliveryAddress()) : null)
                .build();
    }
}
