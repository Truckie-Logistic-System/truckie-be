package capstone_project.service.mapper.seal;

import capstone_project.dtos.response.seal.OrderSealResponse;
import capstone_project.dtos.response.seal.SealResponse;
import capstone_project.entity.order.order.OrderSealEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderSealMapper {

    public OrderSealResponse toResponse(OrderSealEntity entity) {
        if (entity == null) {
            return null;
        }

        // Create SealResponse directly from OrderSealEntity
        SealResponse sealResponse = new SealResponse(
            entity.getId(),
            entity.getSealCode(),
            entity.getDescription(),
            entity.getStatus(),
            entity.getCreatedAt(),
            entity.getModifiedAt()
        );

        return new OrderSealResponse(
            entity.getId(),
            entity.getVehicleAssignment() != null ? entity.getVehicleAssignment().getId() : null,
            entity.getVehicleAssignment() != null ? entity.getVehicleAssignment().getTrackingCode() : null,
            sealResponse,
            entity.getSealDate(),
            entity.getDescription(),
            entity.getSealAttachedImage(),
            entity.getSealRemovalTime(),
            entity.getSealRemovalReason(),
            entity.getStatus(),
            entity.getCreatedAt()
        );
    }
}

