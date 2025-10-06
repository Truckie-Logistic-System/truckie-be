package capstone_project.dtos.response.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO cho lịch sử hành trình (JourneyHistory) với đầy đủ thông tin về các segment
 */
public record JourneyHistoryResponse(
        UUID id,
        String journeyName,
        String journeyType,
        String status,
        Long totalTollFee,
        Integer totalTollCount,
        Double totalDistance,
        String reasonForReroute,
        UUID vehicleAssignmentId,
        List<JourneySegmentResponse> journeySegments,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt
) {}
