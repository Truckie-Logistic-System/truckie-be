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
public class TopStaffResponse {
    private UUID staffId;
    private String name;
    private String email;
    private Long resolvedIssues;
    private String avatarUrl;
}
