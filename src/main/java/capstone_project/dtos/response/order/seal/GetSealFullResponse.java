package capstone_project.dtos.response.order.seal;

import java.util.List;

public record GetSealFullResponse(
        GetSealResponse getSealResponse,
        List<GetOrderDetailSealResponse> getOrderDetailSealResponses
) {
}
