package capstone_project.dtos.response.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopDriverResponse {
    private UUID driverId;
    private String name;
    private String email;
    private Long acceptedTrips;
    private String avatarUrl;
}
