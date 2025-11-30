package capstone_project.dtos.response.user;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class PenaltyHistoryResponse {
    private UUID id;
    private String violationType;
    private LocalDate penaltyDate;
    private String trafficViolationRecordImageUrl;
    private UUID driverId;
    private UUID vehicleAssignmentId;
}