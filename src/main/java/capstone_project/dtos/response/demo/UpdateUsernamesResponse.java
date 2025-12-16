package capstone_project.dtos.response.demo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Response DTO for username update operation
 * Contains summary of updated usernames by role
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUsernamesResponse {
    
    /**
     * Total number of customers updated
     */
    private Integer totalCustomersUpdated;
    
    /**
     * Total number of drivers updated
     */
    private Integer totalDriversUpdated;
    
    /**
     * Total number of staff updated
     */
    private Integer totalStaffUpdated;
    
    /**
     * Total number of users updated across all roles
     */
    private Integer totalUsersUpdated;
    
    /**
     * Map of old username to new username for verification
     * Key: old username, Value: new username
     */
    private Map<String, String> usernameChanges;
    
    /**
     * Success message
     */
    private String message;
    
    /**
     * Execution time in milliseconds
     */
    private Long executionTimeMs;
}
