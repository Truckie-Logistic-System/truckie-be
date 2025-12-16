package capstone_project.dtos.response.demo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemoUsersGenerationResponse {
    private int totalCustomersCreated;
    private int totalDriversCreated;
    private int totalStaffCreated;
    private int totalUsersCreated;
    private Map<String, Integer> usersByDate;
    private String message;
}
