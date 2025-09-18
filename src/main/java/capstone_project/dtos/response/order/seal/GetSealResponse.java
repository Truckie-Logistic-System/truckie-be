package capstone_project.dtos.response.order.seal;

import capstone_project.dtos.response.order.GetOrderDetailResponse;

import java.util.List;
import java.util.UUID;

public record GetSealResponse(
        UUID id,
        String sealCode,
        String description,
        String status

) {
}
