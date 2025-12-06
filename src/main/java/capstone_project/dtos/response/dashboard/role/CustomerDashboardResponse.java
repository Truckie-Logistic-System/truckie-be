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
public class CustomerDashboardResponse {
    
    // Order Overview
    private OrderSummary orderSummary;
    
    // Order Status Distribution
    private Map<String, Long> orderStatusDistribution;
    
    // Delivery Performance
    private DeliveryPerformance deliveryPerformance;
    
    // Financial Overview
    private FinancialSummary financialSummary;
    
    // Active Orders
    private List<ActiveOrderItem> activeOrders;
    
    // Issues & Actions Needed
    private ActionsSummary actionsSummary;
    
    // Recent Activity
    private List<ActivityItem> recentActivity;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderSummary {
        private long totalOrders;
        private long totalOrderDetails;
        private long inTransitPackages; // PICKING_UP, ON_DELIVERED, ONGOING_DELIVERED
        private long deliveredPackages; // DELIVERED
        private long cancelledPackages; // CANCELLED
        private long problemPackages; // IN_TROUBLES, COMPENSATION, RETURNING, RETURNED
        private long pendingOrders; // For active orders section
        private long inProgressOrders; // For active orders section
        private double successRate; // Delivered / (Delivered + Cancelled + Problem)
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeliveryPerformance {
        private long successfulDeliveries;
        private long failedDeliveries;
        private double successRate;
        private long issueCount;
        private double issueRate;
        private List<TrendDataPoint> trendData;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendDataPoint {
        private String date;
        private long deliveredCount;
        private double onTimePercentage;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FinancialSummary {
        private BigDecimal totalPaid;
        private BigDecimal pendingPayment;
        private BigDecimal totalRefunded;
        private BigDecimal totalContractValue;
        private long contractsPendingSignature; // CONTRACT_DRAFT
        private long contractsAwaitingDeposit; // CONTRACT_SIGNED
        private long contractsAwaitingFullPayment; // ASSIGNED_TO_DRIVER
        private long contractsAwaitingReturnFee; // Orders with return fee condition
        private long contractsSigned;
        private long contractsCancelled;
        private List<ContractValueTrend> contractValueTrend;
        private List<TransactionTrend> transactionTrend;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActiveOrderItem {
        private String orderId;
        private String orderCode;
        private String status;
        private String pickupAddress;
        private String deliveryAddress;
        private int totalPackages;
        private int deliveredPackages;
        private String estimatedDelivery;
        private String currentLocation;
        private boolean hasIssue;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActionsSummary {
        private long contractsToSign;
        private long paymentsNeeded;
        private long issuesNeedingResponse;
        private List<ActionItem> actionItems;
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
        private long total;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActionItem {
        private String type; // CONTRACT_SIGN, DEPOSIT_PAYMENT, FULL_PAYMENT, RETURN_FEE_PAYMENT, ISSUE_RESPONSE
        private String id;
        private String orderId;
        private String orderCode;
        private String title;
        private String description;
        private String deadline;
        private String urgency; // HIGH, MEDIUM, LOW
        private BigDecimal amount; // For payment actions
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityItem {
        private String type;
        private String title;
        private String description;
        private String timestamp;
        private String orderId;
        private String relatedOrderCode;
        private String orderStatus;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContractValueTrend {
        private String date;
        private long contractCount;
        private BigDecimal totalValue;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionTrend {
        private String date;
        private BigDecimal amount;
        private long transactionCount;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentIssue {
        private String issueId;
        private String issueType;
        private String description;
        private String status;
        private String reportedAt;
        private String orderCode;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopRecipient {
        private String recipientName;
        private String recipientPhone;
        private String recipientAddress;
        private long totalPackages;
        private long successfulPackages;
        private long failedPackages;
        private double successRate;
    }
    
    // New fields for dashboard
    private List<RecentIssue> recentIssues;
    private List<TopRecipient> topRecipients;
    private Map<String, Long> orderDetailStatusDistribution;
    
    // Package status trend for visualization
    private List<PackageStatusTrend> packageStatusTrend;
}
