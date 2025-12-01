package capstone_project.dtos.response.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for customer overview in chat (for staff view)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerOverviewResponse {
    
    // User info
    private UUID userId;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String imageUrl;
    private Boolean gender;
    private LocalDate dateOfBirth;
    private LocalDateTime memberSince;
    
    // Customer info
    private UUID customerId;
    private String companyName;
    private String representativeName;
    private String representativePhone;
    private String businessLicenseNumber;
    private String businessAddress;
    private String customerStatus;
    
    // Order statistics
    private Integer totalOrders;
    private Integer successfulOrders;
    private Double successRate;
    private Integer cancelledOrders;
    private Double cancelRate;
    private Integer issuesCount;
    private BigDecimal totalSpent;
    
    // Recent orders
    private List<RecentOrderInfo> recentOrders;
    
    // Active orders (PICKING_UP and beyond)
    private List<ActiveOrderInfo> activeOrders;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentOrderInfo {
        private UUID orderId;
        private String orderCode;
        private String status;
        private String receiverName;
        private String pickupAddress;
        private String deliveryAddress;
        private Integer totalQuantity;
        private LocalDateTime createdAt;
        private Boolean isActive; // PICKING_UP and beyond
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActiveOrderInfo {
        private UUID orderId;
        private String orderCode;
        private String status;
        private String receiverName;
        private String driverName;
        private String trackingCode;
        private LocalDateTime createdAt;
    }
}
