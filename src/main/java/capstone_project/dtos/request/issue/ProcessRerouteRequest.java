package capstone_project.dtos.request.issue;

import capstone_project.dtos.request.order.RouteSegmentInfo;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Request DTO for staff to process REROUTE issue
 * Staff provides new route segments to replace affected segment
 */
public record ProcessRerouteRequest(
        @NotNull
        UUID issueId,
        
        @NotNull
        List<RouteSegmentInfo> newRouteSegments, // Route mới thay thế segment gặp sự cố
        
        Long totalTollFee, // Tổng phí BOT cho toàn bộ journey mới
        
        Integer totalTollCount, // Số lượng trạm BOT mới
        
        BigDecimal totalDistance // Tổng khoảng cách journey mới
) {
}
