package capstone_project.service.mapper.order;

import capstone_project.dtos.response.order.PackingProofImageResponse;
import capstone_project.entity.order.confirmation.PackingProofImageEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class PackingProofImageMapper {

    public PackingProofImageResponse toPackingProofImageResponse(PackingProofImageEntity entity) {
        if (entity == null) {
            return null;
        }
        return new PackingProofImageResponse(
                entity.getId(),
                entity.getImageUrl(),
                entity.getDescription(),
                entity.getCreatedAt(),
                entity.getModifiedAt(),
                entity.getVehicleAssignmentEntity() != null ? entity.getVehicleAssignmentEntity().getId() : null
        );
    }

    public List<PackingProofImageResponse> toPackingProofImageResponses(List<PackingProofImageEntity> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream()
                .map(this::toPackingProofImageResponse)
                .collect(Collectors.toList());
    }
}
