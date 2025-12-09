package capstone_project.dtos.response.dashboard.role;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardResponse {
    
    // AI Summary
    private String aiSummary;
    
    // KPI Cards
    private KpiSummary kpiSummary;
    
    // Order & Revenue Trends
    private List<TrendDataPoint> orderTrend;
    private List<TrendDataPoint> revenueTrend;
    
    // On-time vs Late
    private DeliveryPerformance deliveryPerformance;
    
    // Issues & Refunds
    private IssueRefundSummary issueRefundSummary;
    
    // Top Performers
    private List<TopPerformer> topCustomers;
    private List<TopPerformer> topDrivers;
    private List<TopPerformer> topStaff;
    
    // Fleet Health
    private FleetHealthSummary fleetHealth;
    
    // Order Status Distribution
    private Map<String, Long> orderStatusDistribution;
    
    // User Registration Time Series
    private RegistrationData registrationData;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KpiSummary {
        private long totalOrders;
        private long totalOrderDetails;
        private BigDecimal totalRevenue;
        private double onTimePercentage;
        private double issueRate;
        private long newCustomers;
        private BigDecimal refundAmount;
        
        // Comparisons with previous period
        private Double orderGrowth;
        private Double revenueGrowth;
        private Double onTimeGrowthChange;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendDataPoint {
        private String label; // date or period label
        private long count;
        private BigDecimal amount;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeliveryPerformance {
        private long onTimeCount;
        private long lateCount;
        private double onTimePercentage;
        private double latePercentage;
        private List<TrendDataPoint> trend;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IssueRefundSummary {
        private long totalIssues;
        private long openIssues;
        private long resolvedIssues;
        private long pendingRefunds;
        private long completedRefunds;
        private BigDecimal totalRefundAmount;
        private Map<String, Long> issuesByType;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopPerformer {
        private String id;
        private String name;
        private String companyName;
        private long orderCount;
        private BigDecimal revenue;
        private double onTimePercentage;
        private int rank;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FleetHealthSummary {
        private long totalVehicles;
        private long activeVehicles;
        private long inMaintenanceVehicles;
        private long pendingMaintenanceVehicles;
        private long overdueMaintenanceVehicles;
        private BigDecimal averageFuelConsumption;
        private List<MaintenanceAlert> upcomingMaintenances;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MaintenanceAlert {
        private String vehicleId;
        private String licensePlate;
        private String maintenanceType;
        private String dueDate;
        private boolean isOverdue;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegistrationData {
        private List<TrendDataPoint> customerRegistrations;
        private List<TrendDataPoint> staffRegistrations;
        private List<TrendDataPoint> driverRegistrations;
    }
}
