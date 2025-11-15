package capstone_project.dtos.request.issue;

import capstone_project.dtos.request.order.RouteSegmentInfo;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Request DTO for staff to process ORDER_REJECTION issue
 * This will create a transaction and new route for returning goods
 */
public record ProcessOrderRejectionRequest(
        @NotNull
        UUID issueId,
        
        BigDecimal adjustedReturnFee, // Giá ưu đãi (optional, nếu null thì dùng calculated fee)
        
        @NotNull
        List<RouteSegmentInfo> routeSegments, // Route mới: carrier → pickup → delivery → pickup → carrier
        
        Long totalTollFee, // Tổng phí BOT cho route mới
        
        Integer totalTollCount, // Số lượng trạm BOT
        
        BigDecimal totalDistance // Tổng khoảng cách route mới
) {
}
