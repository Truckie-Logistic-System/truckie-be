package capstone_project.dtos.response.demo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO summarizing generated/cleared demo data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemoDataSummary {
    
    private Integer year;
    private Integer usersCreated;
    private Integer customersCreated;
    private Integer staffCreated;
    private Integer driversCreated;
    private Integer ordersCreated;
    private Integer orderDetailsCreated;
    private Integer vehicleAssignmentsCreated;
    private Integer contractsCreated;
    private Integer transactionsCreated;
    private Integer refundsCreated;
    private Integer issuesCreated;
    private Integer vehiclesCreated;
    private Integer maintenancesCreated;
    private Integer penaltiesCreated;
    private Integer fuelConsumptionsCreated;
    
    // For clear operation
    private Integer totalRecordsDeleted;
    
    private String message;
    private Long executionTimeMs;
}
