package capstone_project.dtos.request.order;

import capstone_project.common.enums.JourneyHistoryStatusEnum;
import capstone_project.common.enums.VehicleStatusEnum;
import capstone_project.common.enums.enumValidator.EnumValidator;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class JourneyHistoryRequest {

    @NotNull(message = "Start location is required")
    private BigDecimal startLocation;

    @NotNull(message = "End location is required")
    private BigDecimal endLocation;

    @NotNull(message = "Start time is required")
    @PastOrPresent(message = "Start time cannot be in the future")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    @FutureOrPresent(message = "End time cannot be in the past")
    private LocalDateTime endTime;

    @NotBlank(message = "Status is required")
    @EnumValidator(enumClass = JourneyHistoryStatusEnum.class, message = "Invalid status for journey history")
    private String status;

    @NotNull(message = "Total distance is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Total distance must be greater than 0")
    private BigDecimal totalDistance;

    @NotNull(message = "Reported incident flag is required")
    private Boolean isReportedIncident;

    @NotNull
    private UUID orderId;
}
