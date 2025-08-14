package capstone_project.dtos.response.user;

import lombok.Builder;
import lombok.Data;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class PenaltyHistoryResponse {
    private UUID id;
    private String violationType;
    private String violationDescription;
    private BigDecimal penaltyAmount;
    private LocalDate penaltyDate;
    private String location;
    private String status;
    private LocalDateTime paymentDate;
    private String disputeReason;
    private UUID driverId;
    private UUID vehicleAssignmentId;
}