package capstone_project.dtos.response.order.seal;

import capstone_project.dtos.response.order.GetOrderDetailResponse;

import java.time.LocalDateTime;
import java.util.UUID;

public record GetOrderDetailSealResponse (
        UUID id,
        String description,
        LocalDateTime sealDate,
        String status,
        UUID sealId,
        GetOrderDetailResponse orderDetail
) {

}
