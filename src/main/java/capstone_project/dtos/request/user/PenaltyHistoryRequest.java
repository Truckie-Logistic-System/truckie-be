package capstone_project.dtos.request.user;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class PenaltyHistoryRequest {
    private String violationType;
    private String violationDescription;
    private BigDecimal penaltyAmount;
    private LocalDate  penaltyDate;
    private String location;
    private String status;
    private UUID driverId;
    private UUID vehicleAssignmentId;
}
