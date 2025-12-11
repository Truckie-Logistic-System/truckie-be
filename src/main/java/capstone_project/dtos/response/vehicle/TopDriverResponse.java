package capstone_project.dtos.response.vehicle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopDriverResponse {
    private String driverId;
    private String driverName;
    private String driverPhoneNumber;
    private long tripCount;
    private String driverStatus;
}
