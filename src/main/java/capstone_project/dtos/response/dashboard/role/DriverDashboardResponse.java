package capstone_project.dtos.response.dashboard.role;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverDashboardResponse {
    
    // Key Metrics
    private int completedTripsCount;
    private int incidentsCount;
    private int trafficViolationsCount;
    
    // Trip Trend Data (for line chart - completed trips only)
    private List<TripTrendPoint> tripTrend;
    
    // Recent Orders (Vehicle Assignments for driver)
    private List<RecentOrder> recentOrders;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TripTrendPoint {
        private String label; // Date label (dd/MM or Week X or Month name)
        private int tripsCompleted; // Completed trips only
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentOrder {
        private String orderId; // Order ID
        private String orderCode; // Order code from OrderEntity
        private String status; // Order status
        private String receiverName; // Receiver name
        private String receiverPhone; // Receiver phone
        private String createdDate; // Order created date
        private String trackingCode; // Order detail tracking code
    }
}
