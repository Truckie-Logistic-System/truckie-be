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
public class VehicleDetailEnhancedResponse {
    private String id;
    private String licensePlateNumber;
    private String model;
    private String manufacturer;
    private Integer year;
    private String status;
    private VehicleTypeResponse vehicleType;
    
    // Recent assignments (paginated - 3 items per call)
    private PaginatedAssignmentsResponse recentAssignments;
    
    // Recent maintenances (paginated - 3 items per call)
    private PaginatedServiceRecordsResponse recentMaintenances;
    
    // Top 3 drivers for this vehicle (driver 1 position)
    private List<TopDriverResponse> topDrivers;
    
    // Penalties related to this vehicle
    private List<VehiclePenaltyResponse> penalties;
}
