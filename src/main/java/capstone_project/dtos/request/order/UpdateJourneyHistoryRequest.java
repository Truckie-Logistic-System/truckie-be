package capstone_project.dtos.request.order;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class UpdateJourneyHistoryRequest {
    private BigDecimal startLocation;
    private BigDecimal endLocation;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private BigDecimal totalDistance;
    private Boolean isReportedIncident;

    private UUID orderId;
}
