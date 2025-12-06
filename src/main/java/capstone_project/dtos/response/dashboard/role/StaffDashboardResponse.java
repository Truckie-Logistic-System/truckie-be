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
public class StaffDashboardResponse {
    
    // Operational Summary
    private OperationalSummary operationalSummary;
    
    // Trip Status
    private Map<String, Long> tripStatusDistribution;
    private List<TripAlert> tripAlerts;
    
    // Issues
    private IssueSummary issueSummary;
    private List<IssueItem> pendingIssues;
    
    // Contracts & Transactions
    private FinancialSummary financialSummary;
    
    // Fleet Status
    private FleetStatus fleetStatus;
    
    // Driver Performance section removed
    
    // === NEW FIELDS FOR ENHANCED DASHBOARD ===
    
    // Package/Order Summary (like customer dashboard)
    private PackageSummary packageSummary;
    
    // Trend data for graphs
    private List<TripCompletionTrend> tripCompletionTrend;
    private List<IssueTypeTrend> issueTypeTrend;
    private List<ContractTrend> contractTrend;
    private List<TransactionTrend> transactionTrend;
    private List<RefundTrend> refundTrend;
    private List<PackageStatusTrend> packageStatusTrend;
    private List<RevenueCompensationTrend> revenueCompensationTrend;
    
    // Recent orders
    private List<RecentOrderItem> recentOrders;
    
    // Pending orders (PROCESSING, ON_PLANNING)
    private List<PendingOrderItem> pendingOrders;
    
    // Top customers
    private List<TopCustomerItem> topCustomers;
    
    // Top drivers
    private List<TopDriverItem> topDrivers;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OperationalSummary {
        private long totalTrips;
        private long activeTrips;
        private long completedTrips;
        private long delayedTrips;
        private long totalOrderDetails;
        private long pendingOrderDetails;
        private long deliveringOrderDetails;
        private long completedOrderDetails;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TripAlert {
        private String tripId;
        private String trackingCode;
        private String vehiclePlate;
        private String driverName;
        private String status;
        private String alertType; // DELAYED, SLA_RISK, ISSUE_REPORTED
        private String message;
        private String estimatedDelay;
        private String issueId; // For navigation to issue detail
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IssueSummary {
        private long totalIssues;
        private long openIssues;
        private long inProgressIssues;
        private long resolvedIssues;
        private long pendingRefunds;
        private Map<String, Long> issuesByCategory;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IssueItem {
        private String issueId;
        private String description;
        private String category;
        private String status;
        private String reportedAt;
        private String tripTrackingCode;
        private String orderCode;
        private boolean isUrgent;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FinancialSummary {
        private long totalContracts;
        private long pendingContracts;
        private long paidContracts;  // Renamed from signedContracts - contracts with PAID status
        private long completedContracts;
        private BigDecimal totalContractValue;
        private long totalTransactions;
        private long pendingTransactions;
        private long completedTransactions;
        private BigDecimal transactionAmount;
        private BigDecimal totalRefunded;  // Total refunded amount for completed refunds
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FleetStatus {
        private long totalVehicles;
        private long availableVehicles;
        private long inUseVehicles;
        private long inMaintenanceVehicles;
        private List<MaintenanceAlert> maintenanceAlerts;
        private List<FuelAlert> fuelAlerts;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MaintenanceAlert {
        private String vehicleId;
        private String licensePlate;
        private String maintenanceType;
        private String scheduledDate;
        private String status;
        private boolean isOverdue;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FuelAlert {
        private String vehicleId;
        private String licensePlate;
        private String tripTrackingCode;
        private BigDecimal expectedConsumption;
        private BigDecimal actualConsumption;
        private double deviationPercentage;
    }
    
    // DriverPerformanceItem class removed - no longer needed
    
    // === NEW DTOs FOR ENHANCED DASHBOARD ===
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PackageSummary {
        private long totalOrderDetails;
        private long inTransitPackages;
        private long deliveredPackages;
        private long cancelledPackages;
        private long problemPackages;
        private long totalOrders;
        private double successRate;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TripCompletionTrend {
        private String date;
        private long completedTrips;
        private long activeTrips;
        private long totalTrips;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IssueTypeTrend {
        private String date;
        private String issueType;
        private long count;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContractTrend {
        private String date;
        private long createdCount;
        private long paidCount;
        private long cancelledCount;
        private BigDecimal totalValue;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionTrend {
        private String date;
        private BigDecimal paidAmount;
        private long paidCount;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefundTrend {
        private String date;
        private long refundCount;
        private BigDecimal refundAmount;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PackageStatusTrend {
        private String date;
        private long inTransit;
        private long delivered;
        private long cancelled;
        private long problem;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentOrderItem {
        private String orderId;
        private String orderCode;
        private String senderName;
        private String senderCompany;
        private long totalPackages;
        private long deliveredPackages;
        private String status;
        private boolean hasIssue;
        private String createdAt;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopCustomerItem {
        private String customerId;
        private String customerName;
        private String companyName;
        private long totalOrders;
        private long totalPackages;
        private BigDecimal totalRevenue;
        private double successRate;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PendingOrderItem {
        private String orderId;
        private String orderCode;
        private String senderName;
        private String senderCompany;
        private String senderPhone;
        private long totalPackages;
        private String status;
        private String pickupAddress;
        private String deliveryAddress;
        private String createdAt;
        private String note;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RevenueCompensationTrend {
        private String date;
        private BigDecimal revenue;      // Tổng tiền thu (từ transactions)
        private BigDecimal compensation; // Tổng tiền đền bù (từ refunds)
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopDriverItem {
        private String driverId;
        private String driverName;
        private String phone;
        private long completedTrips;
        private long totalTrips;
        private double onTimePercentage;
        private double completionRate;
    }
}
