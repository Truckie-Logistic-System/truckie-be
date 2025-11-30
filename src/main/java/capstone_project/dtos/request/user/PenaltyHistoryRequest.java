package capstone_project.dtos.request.user;

import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class PenaltyHistoryRequest {
    private String violationType;
    private LocalDate penaltyDate;
    private String trafficViolationRecordImageUrl;
    private UUID driverId;
    private UUID vehicleAssignmentId;
}
