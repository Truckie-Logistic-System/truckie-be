package capstone_project.dtos.response.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Response DTO for duplicate user cleanup operation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DuplicateUserCleanupResponse {
    
    private int totalDuplicateGroupsFound;
    private int totalUsersDeleted;
    private int totalCustomersDeleted;
    private int totalDriversDeleted;
    private List<DeletedUserInfo> deletedUsers;
    private List<String> errors;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeletedUserInfo {
        private UUID userId;
        private String username;
        private String email;
        private String roleName;
        private boolean hadCustomerRecord;
        private boolean hadDriverRecord;
    }
}
