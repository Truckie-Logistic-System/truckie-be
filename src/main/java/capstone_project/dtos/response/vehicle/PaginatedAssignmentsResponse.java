package capstone_project.dtos.response.vehicle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginatedAssignmentsResponse {
    private List<VehicleAssignmentResponse> items;
    private int page;
    private int pageSize;
    private long totalItems;
    private int totalPages;
}
