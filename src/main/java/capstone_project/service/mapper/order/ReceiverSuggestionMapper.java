package capstone_project.service.mapper.order;

import capstone_project.dtos.response.order.ReceiverSuggestionResponse;
import capstone_project.entity.order.order.OrderEntity;
import org.springframework.stereotype.Component;

@Component
public class ReceiverSuggestionMapper {

    /**
     * Map an OrderEntity to ReceiverSuggestionResponse
     *
     * @param entity OrderEntity to map
     * @return ReceiverSuggestionResponse
     */
    public ReceiverSuggestionResponse toDto(OrderEntity entity) {
        if (entity == null) {
            return null;
        }

        String partialAddress = "";
        if (entity.getDeliveryAddress() != null) {
            StringBuilder sb = new StringBuilder();
            if (entity.getDeliveryAddress().getStreet() != null) {
                sb.append(entity.getDeliveryAddress().getStreet());
            }

            if (entity.getDeliveryAddress().getWard() != null) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(entity.getDeliveryAddress().getWard());
            }

            if (entity.getDeliveryAddress().getProvince() != null) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(entity.getDeliveryAddress().getProvince());
            }

            partialAddress = sb.toString();
        }

        return ReceiverSuggestionResponse.builder()
                .orderId(entity.getId())
                .receiverName(entity.getReceiverName())
                .receiverPhone(entity.getReceiverPhone())
                .partialAddress(partialAddress)
                .orderDate(entity.getCreatedAt())
                .build();
    }
}
