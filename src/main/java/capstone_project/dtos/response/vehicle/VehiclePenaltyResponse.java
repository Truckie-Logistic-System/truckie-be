package capstone_project.dtos.response.vehicle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehiclePenaltyResponse {
    private String id;
    private String penaltyType;
    private String description;
    private BigDecimal fineAmount;
    private LocalDateTime violationDate;
    private String status;
    private String driverName;
    private String assignmentId;
}
