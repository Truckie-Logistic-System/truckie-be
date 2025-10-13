package capstone_project.dtos.response.order.contract;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record PackedDetailResponse(
        String orderDetailId,
        BigDecimal x,
        BigDecimal y,
        BigDecimal z,
        BigDecimal length,
        BigDecimal width,
        BigDecimal height,
        String orientation,
        int orientation_X,
        int orientation_Y,
        int orientation_Z
) {
}
