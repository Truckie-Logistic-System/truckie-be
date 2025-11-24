package capstone_project.service.services.ai;

import capstone_project.entity.order.order.OrderEntity;
import capstone_project.entity.order.order.OrderDetailEntity;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.repository.repositories.order.order.OrderRepository;
import capstone_project.repository.repositories.user.CustomerRepository;
import capstone_project.repository.repositories.issue.IssueRepository;
import capstone_project.repository.repositories.order.transaction.TransactionRepository;
import capstone_project.entity.issue.IssueEntity;
import capstone_project.entity.order.transaction.TransactionEntity;
import capstone_project.entity.user.customer.CustomerEntity;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for customer analytics and insights
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerAnalyticsService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final IssueRepository issueRepository;
    private final TransactionRepository transactionRepository;

    /**
     * Generate spending analytics for period
     * @param userId User ID
     * @param period "month", "quarter", "year"
     */
    public String generateSpendingAnalytics(String userId, String period) {
        log.info("🔍 DEBUG: generateSpendingAnalytics called with userId: {}, period: {}", userId, period);
        
        if (userId == null || userId.isEmpty()) {
            return "⚠️ Vui lòng đăng nhập để xem thống kê.\n\n";
        }

        try {
            UUID userUUID = UUID.fromString(userId);
            log.info("🔍 DEBUG: Parsed UUID: {}", userUUID);
            
            // Find orders by sender (customer) - First find customer by user ID, then get orders
            Optional<CustomerEntity> customerOpt = customerRepository.findByUserId(userUUID);
            if (customerOpt.isEmpty()) {
                log.warn("⚠️ No customer found for user_id: {}", userUUID);
                return "# 📊 THỐNG KÊ ĐẶT HÀNG\n\n" +
                       "⚠️ Không tìm thấy thông tin khách hàng.\n\n";
            }
            
            CustomerEntity customer = customerOpt.get();
            UUID customerId = customer.getId();
            log.info("📊 DEBUG: Found customer_id: {} for user_id: {}", customerId, userUUID);
            
            log.info("📊 DEBUG: Querying orders with sender_id: {}", customerId);
            List<OrderEntity> allOrders = orderRepository.findBySenderIdOrderByCreatedAtDesc(customerId);
            log.info("📊 DEBUG: Found {} total orders for customer {}", allOrders.size(), customerId);
            
            // Additional debug: Check if ANY orders exist in database (reuse from OrderTrackingService)
            if (allOrders.isEmpty()) {
                log.info("📊 DEBUG: Checking all orders in database to debug...");
                List<OrderEntity> allOrdersInDb = orderRepository.findAll();
                log.info("📊 DEBUG: Total orders in database: {}", allOrdersInDb.size());
                if (!allOrdersInDb.isEmpty()) {
                    allOrdersInDb.subList(0, Math.min(5, allOrdersInDb.size())).forEach(order -> 
                        log.info("📊 DEBUG: Sample order - sender_id: {}, order_code: {}", 
                            order.getSender() != null ? order.getSender().getId() : "null", 
                            order.getOrderCode())
                    );
                }
            }

            if (allOrders.isEmpty()) {
                log.info("⚠️ No orders found for analytics, user {}", userId);
                return "**📊 THỐNG KÊ ĐẶT HÀNG**\n\n" +
                       "Bạn chưa có đơn hàng nào trong hệ thống.\n\n" +
                       "💡 Tạo đơn hàng đầu tiên để bắt đầu sử dụng dịch vụ!\n\n";
            }

            StringBuilder info = new StringBuilder();
            info.append("**📊 THỐNG KÊ ĐẶT HÀNG**\n\n");

            // Get date ranges
            LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
            LocalDateTime startDate = getStartDate(now, period);
            
            // Filter orders in period
            List<OrderEntity> periodOrders = allOrders.stream()
                    .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().isAfter(startDate))
                    .collect(Collectors.toList());

            info.append(String.format("## %s\n\n", getPeriodName(period, startDate)));
            info.append(generateOrderStatistics(periodOrders, customerId));
            
            // Comparison with previous period
            LocalDateTime prevStartDate = getPreviousPeriodStartDate(startDate, period);
            List<OrderEntity> prevPeriodOrders = allOrders.stream()
                    .filter(o -> o.getCreatedAt() != null 
                            && o.getCreatedAt().isAfter(prevStartDate)
                            && o.getCreatedAt().isBefore(startDate))
                    .collect(Collectors.toList());
            
            log.info("🔍 DEBUG: Previous period orders found: {} for period: {} (date range: {} to {})", 
                    prevPeriodOrders.size(), period, prevStartDate, startDate);

            info.append(generateComparison(periodOrders, prevPeriodOrders, period, startDate));
            
            // Breakdown by vehicle type
            info.append(generateVehicleBreakdown(periodOrders));
            
            // Suggestions
            info.append(generateSuggestions(allOrders, periodOrders));

            log.info("✅ Generated analytics for user: {}, period: {}", userId, period);
            log.info("🔍 ANALYTICS OUTPUT:\n{}", info.toString());
            return info.toString();

        } catch (IllegalArgumentException e) {
            log.error("❌ Invalid user ID format: {}", userId);
            return "⚠️ ID người dùng không hợp lệ.\n\n";
        } catch (Exception e) {
            log.error("❌ Error generating analytics", e);
            return "⚠️ Không thể tải thống kê. Vui lòng thử lại sau.\n\n";
        }
    }

    
    /**
     * Translate OrderDetail status to Vietnamese
     */
    private String translateStatus(String status) {
        if (status == null) return "Không xác định";
        
        return switch (status) {
            case "DELIVERED" -> "Đã giao hàng";
            case "CANCELLED" -> "Đã hủy";
            case "RETURNED" -> "Đã trả hàng";
            case "COMPENSATION" -> "Đã bồi thường";
            case "PENDING" -> "Chờ xử lý";
            case "ON_PLANNING" -> "Đang lập kế hoạch";
            case "ASSIGNED_TO_DRIVER" -> "Đã giao cho tài xế";
            case "PICKING_UP" -> "Đang lấy hàng";
            case "ON_DELIVERED" -> "Đang giao hàng";
            case "ONGOING_DELIVERED" -> "Đang trên đường giao";
            case "IN_TROUBLES" -> "Đang gặp sự cố";
            case "RETURNING" -> "Đang trả về";
            default -> status;
        };
    }

    /**
     * Translate vehicle type to Vietnamese
     */
    private String translateVehicleType(String vehicleType) {
        if (vehicleType == null) return "Không xác định";
        
        return switch (vehicleType) {
            case "TRUCK_10_TON" -> "Xe tải 10 tấn";
            case "TRUCK_7_TON" -> "Xe tải 7 tấn";
            case "TRUCK_5_TON" -> "Xe tải 5 tấn";
            case "TRUCK_3_5_TON" -> "Xe tải 3.5 tấn";
            default -> vehicleType;
        };
    }

    /**
     * Generate order statistics (now counting packages/order details with priority metrics)
     */
    private String generateOrderStatistics(List<OrderEntity> orders, UUID customerId) {
        StringBuilder info = new StringBuilder();

        // Get all order details (packages) from customer's orders
        List<OrderDetailEntity> customerOrderDetails = orders.stream()
                .filter(order -> order.getOrderDetailEntities() != null)
                .flatMap(order -> order.getOrderDetailEntities().stream())
                .collect(Collectors.toList());

        // Calculate priority metrics
        long deliveredPackages = customerOrderDetails.stream()
                .filter(orderDetail -> "DELIVERED".equals(orderDetail.getStatus()))
                .count();
        
        long cancelledPackages = customerOrderDetails.stream()
                .filter(orderDetail -> "CANCELLED".equals(orderDetail.getStatus()))
                .count();
        
        long returnedPackages = customerOrderDetails.stream()
                .filter(orderDetail -> "RETURNED".equals(orderDetail.getStatus()))
                .count();
        
        long compensatedPackages = customerOrderDetails.stream()
                .filter(orderDetail -> "COMPENSATION".equals(orderDetail.getStatus()))
                .count();

        // Total weight shipped
        BigDecimal totalWeight = customerOrderDetails.stream()
                .filter(od -> od.getWeightTons() != null)
                .map(OrderDetailEntity::getWeightTons)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Display priority metrics at top
        info.append("\n### 📊 Thống Kê Chính:\n\n");
        info.append(String.format("- **📦 Tổng số kiện hàng**: %d kiện\n", customerOrderDetails.size()));
        info.append(String.format("- **✅ Số kiện hàng giao thành công**: %d kiện\n", deliveredPackages));
        info.append(String.format("- **❌ Số kiện hàng bị hủy**: %d kiện\n", cancelledPackages));
        info.append(String.format("- **🔄 Số kiện hàng bị trả**: %d kiện\n", returnedPackages));
        info.append(String.format("- **💸 Số kiện hàng được đền bù**: %d kiện\n", compensatedPackages));
        
        if (totalWeight.compareTo(BigDecimal.ZERO) > 0) {
            info.append(String.format("- **⚖️ Tổng trọng lượng**: %.2f tấn\n", totalWeight));
        }
        info.append("\n");

        // Comprehensive status breakdown
        Map<String, Long> statusCounts = customerOrderDetails.stream()
                .collect(Collectors.groupingBy(
                        od -> od.getStatus() != null ? od.getStatus() : "UNKNOWN",
                        Collectors.counting()
                ));

        // Always show comprehensive status breakdown with all OrderDetailStatusEnum values
        info.append("### 📈 Chi Tiết Theo Trạng Thái:\n\n");
        
        // Define priority order based on OrderDetailStatusEnum
        String[] priorityOrder = {"DELIVERED", "CANCELLED", "RETURNED", "COMPENSATION", "PENDING", "ON_PLANNING", "ASSIGNED_TO_DRIVER", "PICKING_UP", "ON_DELIVERED", "ONGOING_DELIVERED", "IN_TROUBLES", "RETURNING"};
        
        // Display all priority statuses (show 0 count if not present) with Vietnamese translation
        for (String status : priorityOrder) {
            long count = statusCounts.getOrDefault(status, 0L);
            String vietnameseStatus = translateStatus(status);
            info.append(String.format("- **%s**: %d kiện\n", vietnameseStatus, count));
        }
        
        // Display any remaining statuses (shouldn't be any, but just in case)
        statusCounts.entrySet().stream()
                .filter(entry -> !Arrays.asList(priorityOrder).contains(entry.getKey()))
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    info.append(String.format("- **%s**: %d kiện\n",
                            entry.getKey(),
                            entry.getValue()
                        ));
                });
        info.append("\n");

        // Additional metrics (excluding the ones already shown)
        info.append("\n");
        info.append(generateAdditionalMetrics(customerId, deliveredPackages, cancelledPackages, returnedPackages, compensatedPackages));

        return info.toString();
    }

    /**
     * Generate additional metrics (incidents, payments, cancelled orders - excluding package counts already shown)
     */
    private String generateAdditionalMetrics(UUID customerId, long deliveredPackages, long cancelledPackages, long returnedPackages, long compensatedPackages) {
        StringBuilder info = new StringBuilder();
        
        try {
            // Count incidents - debug approach
            log.info("🔍 DEBUG: Starting incident count for customer: {}", customerId);
            
            List<IssueEntity> allIssues = issueRepository.findAll();
            log.info("🔍 DEBUG: Found {} total issues in database", allIssues.size());
            
            // Follow proper relationship chain: Customer → Orders → OrderDetails → VehicleAssignments → Issues
            List<OrderEntity> customerOrders = orderRepository.findBySenderIdOrderByCreatedAtDesc(customerId);
            log.info("🔍 DEBUG: Found {} customer orders", customerOrders.size());
            
            // Get all VehicleAssignments from customer's orderDetails
            List<VehicleAssignmentEntity> customerVehicleAssignments = customerOrders.stream()
                    .filter(order -> order.getOrderDetailEntities() != null)
                    .flatMap(order -> order.getOrderDetailEntities().stream())
                    .filter(orderDetail -> orderDetail.getVehicleAssignmentEntity() != null)
                    .map(OrderDetailEntity::getVehicleAssignmentEntity)
                    .distinct()
                    .collect(Collectors.toList());
            
            log.info("🔍 DEBUG: Found {} vehicle assignments for customer", customerVehicleAssignments.size());
            
            // Find issues that belong to customer's vehicle assignments
            List<IssueEntity> issues = allIssues.stream()
                    .filter(issue -> {
                        boolean matchesCustomer = issue.getVehicleAssignmentEntity() != null &&
                                customerVehicleAssignments.contains(issue.getVehicleAssignmentEntity());
                        
                        log.info("🔍 DEBUG: Issue {} - hasVehicleAssignment: {}, matchesCustomer: {}", 
                                issue.getId(), 
                                issue.getVehicleAssignmentEntity() != null,
                                matchesCustomer);
                        
                        return matchesCustomer;
                    })
                    .collect(Collectors.toList());
            
            log.info("🔍 DEBUG: Found {} issues for customer {}", issues.size(), customerId);
            
            // Delivered packages, cancelled packages, returned packages, compensated packages are now passed as parameters
            
            // Calculate total payments - filter for PAID status transactions
            List<TransactionEntity> transactions = transactionRepository.findAll().stream()
                    .filter(transaction -> "PAID".equals(transaction.getStatus()))
                    .collect(Collectors.toList());
            
            BigDecimal totalPayments = transactions.stream()
                    .filter(transaction -> transaction.getAmount() != null)
                    .map(TransactionEntity::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // Count cancelled orders
            long cancelledOrders = orderRepository.findBySenderIdOrderByCreatedAtDesc(customerId).stream()
                    .filter(order -> "CANCELLED".equals(order.getStatus()))
                    .count();
            
            // Format and display metrics (excluding package counts already shown in main section)
            info.append("### 📈 Thống Kê Bổ Sung:\n\n");
            info.append(String.format("- **🚨 Số sự cố đã gặp**: %d\n", issues.size()));
            info.append(String.format("- **💰 Tổng tiền đã thanh toán**: %s VNĐ\n", 
                    NumberFormat.getInstance(Locale.forLanguageTag("vi-VN")).format(totalPayments)));
            info.append(String.format("- **❌ Số đơn hàng bị hủy**: %d\n", cancelledOrders));
            info.append("\n");
            
        } catch (Exception e) {
            log.warn("Could not generate additional metrics: {}", e.getMessage());
            info.append("### 📈 Thống Kê Bổ Sung:\n");
            info.append("- Không thể tải thống kê chi tiết\n\n");
        }
        
        return info.toString();
    }

    /**
     * Generate comparison with previous period (now comparing packages with full status breakdown)
     */
    private String generateComparison(List<OrderEntity> currentOrders, 
                                     List<OrderEntity> previousOrders, 
                                     String period, LocalDateTime currentStartDate) {
        StringBuilder info = new StringBuilder();

        // Get order details from both periods
        List<OrderDetailEntity> currentOrderDetails = currentOrders.stream()
                .filter(order -> order.getOrderDetailEntities() != null)
                .flatMap(order -> order.getOrderDetailEntities().stream())
                .collect(Collectors.toList());

        List<OrderDetailEntity> previousOrderDetails = previousOrders.stream()
                .filter(order -> order.getOrderDetailEntities() != null)
                .flatMap(order -> order.getOrderDetailEntities().stream())
                .collect(Collectors.toList());

        int currentCount = currentOrderDetails.size();
        int previousCount = previousOrderDetails.size();

        LocalDateTime previousStartDate = getPreviousPeriodStartDate(currentStartDate, period);
        String currentPeriodName = getPeriodName(period, currentStartDate);
        String previousPeriodName = getPeriodName(period, previousStartDate);
        
        info.append(String.format("### 📈 So Sánh Với %s:\n", previousPeriodName));
        
        if (previousCount == 0) {
            info.append("Không có dữ liệu kỳ trước để so sánh.\n\n");
            return info.toString();
        }

        // Overall comparison
        double percentChange = ((double) (currentCount - previousCount) / previousCount) * 100;
        String trend = percentChange >= 0 ? "📈 Tăng" : "📉 Giảm";
        
        info.append("**Tổng Quan:**\n");
        info.append(String.format("- %s: %d kiện\n", previousPeriodName, previousCount));
        info.append(String.format("- %s: %d kiện\n", currentPeriodName, currentCount));
        info.append(String.format("- **Thay đổi**: %s%s%.1f%%\n\n", 
                trend,
                percentChange >= 0 ? " +" : " ", 
                Math.abs(percentChange)));

        // Detailed status comparison
        Map<String, Long> currentStatusCounts = currentOrderDetails.stream()
                .collect(Collectors.groupingBy(
                        od -> od.getStatus() != null ? od.getStatus() : "UNKNOWN",
                        Collectors.counting()
                ));
        
        Map<String, Long> previousStatusCounts = previousOrderDetails.stream()
                .collect(Collectors.groupingBy(
                        od -> od.getStatus() != null ? od.getStatus() : "UNKNOWN",
                        Collectors.counting()
                ));
        
        info.append("**Chi Tiết Theo Trạng Thái:**\n\n");
        String[] priorityOrder = {"DELIVERED", "CANCELLED", "RETURNED", "COMPENSATION", "PENDING", "ON_PLANNING", "ASSIGNED_TO_DRIVER", "PICKING_UP", "ON_DELIVERED", "ONGOING_DELIVERED", "IN_TROUBLES", "RETURNING"};
        
        for (String status : priorityOrder) {
            long currentStatusCount = currentStatusCounts.getOrDefault(status, 0L);
            long previousStatusCount = previousStatusCounts.getOrDefault(status, 0L);
            long diff = currentStatusCount - previousStatusCount;
            
            String diffStr = diff > 0 ? "+" + diff : String.valueOf(diff);
            String vietnameseStatus = translateStatus(status);
            info.append(String.format("- **%s**: %d kiện (%s)\n", vietnameseStatus, currentStatusCount, diffStr));
        }
        info.append("\n");

        return info.toString();
    }

    /**
     * Generate vehicle type breakdown
     */
    private String generateVehicleBreakdown(List<OrderEntity> orders) {
        StringBuilder info = new StringBuilder();

        // Get vehicle types from order details
        Map<String, Long> vehicleCounts = new HashMap<>();
        
        for (OrderEntity order : orders) {
            if (order.getOrderDetailEntities() != null) {
                for (OrderDetailEntity detail : order.getOrderDetailEntities()) {
                    if (detail.getVehicleAssignmentEntity() != null 
                            && detail.getVehicleAssignmentEntity().getVehicleEntity() != null
                            && detail.getVehicleAssignmentEntity().getVehicleEntity().getVehicleTypeEntity() != null) {
                        
                        String vehicleType = detail.getVehicleAssignmentEntity()
                                .getVehicleEntity()
                                .getVehicleTypeEntity()
                                .getVehicleTypeName();
                        
                        String vietnameseVehicleType = translateVehicleType(vehicleType);
                        vehicleCounts.merge(vietnameseVehicleType, 1L, Long::sum);
                    }
                }
            }
        }

        if (!vehicleCounts.isEmpty()) {
            info.append("### 🚛 Phân Loại Theo Loại Xe:\n\n");
            vehicleCounts.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .forEach(entry -> {
                        info.append(String.format("- **%s**: %d chuyến\n",
                                entry.getKey(),
                                entry.getValue()
                        ));
                    });
            info.append("\n");
        }

        return info.toString();
    }

    /**
     * Generate smart suggestions (now based on package count)
     */
    private String generateSuggestions(List<OrderEntity> allOrders, List<OrderEntity> periodOrders) {
        StringBuilder info = new StringBuilder();

        // Get package counts for suggestions
        List<OrderDetailEntity> periodOrderDetails = periodOrders.stream()
                .filter(order -> order.getOrderDetailEntities() != null)
                .flatMap(order -> order.getOrderDetailEntities().stream())
                .collect(Collectors.toList());

        info.append("### 💡 Gợi Ý Dành Cho Bạn:\n\n");

        // Basic operational suggestions based on package count
        if (periodOrderDetails.isEmpty()) {
            info.append("- 📝 **Bắt đầu sử dụng**: Tạo đơn hàng đầu tiên để trải nghiệm dịch vụ vận chuyển của chúng tôi!\n");
        } else if (periodOrderDetails.size() <= 3) {
            info.append("- 🚚 **Tăng cường sử dụng**: Bạn có thể tạo thêm kiện hàng để tối ưu chi phí vận chuyển!\n");
        } else {
            info.append("- 📊 **Duy trì tần suất**: Tiếp tục sử dụng dịch vụ thường xuyên để nhận được hỗ trợ tốt nhất!\n");
        }

        // Vehicle usage suggestion
        Map<String, Long> vehicleCounts = new HashMap<>();
        for (OrderEntity order : periodOrders) {
            if (order.getOrderDetailEntities() != null) {
                for (OrderDetailEntity detail : order.getOrderDetailEntities()) {
                    if (detail.getVehicleAssignmentEntity() != null 
                            && detail.getVehicleAssignmentEntity().getVehicleEntity() != null
                            && detail.getVehicleAssignmentEntity().getVehicleEntity().getVehicleTypeEntity() != null) {
                        
                        String vehicleType = detail.getVehicleAssignmentEntity()
                                .getVehicleEntity()
                                .getVehicleTypeEntity()
                                .getVehicleTypeName();
                        
                        vehicleCounts.merge(vehicleType, 1L, Long::sum);
                    }
                }
            }
        }

        if (!vehicleCounts.isEmpty()) {
            String mostUsedVehicle = vehicleCounts.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("");
            
            if (!mostUsedVehicle.isEmpty()) {
                info.append(String.format("- 🚛 **Loại xe ưa thích**: **%s** là loại xe bạn sử dụng nhiều nhất. " +
                           "Tiếp tục sử dụng để tối ưu hiệu quả vận chuyển!\n", mostUsedVehicle));
            }
        }

        info.append("\n💬 Liên hệ **028/3006588** để được tư vấn chi tiết!\n\n");

        return info.toString();
    }

    /**
     * Get start date for period
     */
    private LocalDateTime getStartDate(LocalDateTime from, String period) {
        return switch (period.toLowerCase()) {
            case "day" -> from.withHour(0).withMinute(0).withSecond(0);
            case "week" -> from.with(java.time.DayOfWeek.MONDAY).withHour(0).withMinute(0).withSecond(0);
            case "day_of_week" -> from.with(java.time.DayOfWeek.MONDAY).withHour(0).withMinute(0).withSecond(0);
            case "month" -> from.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            case "quarter" -> {
                int currentMonth = from.getMonthValue();
                int quarterStartMonth = ((currentMonth - 1) / 3) * 3 + 1;
                yield from.withMonth(quarterStartMonth).withDayOfMonth(1)
                        .withHour(0).withMinute(0).withSecond(0);
            }
            case "year" -> from.withDayOfYear(1).withHour(0).withMinute(0).withSecond(0);
            default -> from.minusMonths(1);
        };
    }

    /**
     * Get start date for previous period
     */
    private LocalDateTime getPreviousPeriodStartDate(LocalDateTime currentPeriodStart, String period) {
        return switch (period.toLowerCase()) {
            case "day" -> currentPeriodStart.minusDays(1).withHour(0).withMinute(0).withSecond(0);
            case "week" -> currentPeriodStart.minusWeeks(1).withHour(0).withMinute(0).withSecond(0);
            case "day_of_week" -> currentPeriodStart.minusWeeks(1).withHour(0).withMinute(0).withSecond(0);
            case "month" -> currentPeriodStart.minusMonths(1).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            case "quarter" -> currentPeriodStart.minusMonths(3).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            case "year" -> currentPeriodStart.minusYears(1).withDayOfYear(1).withHour(0).withMinute(0).withSecond(0);
            default -> currentPeriodStart.minusMonths(1);
        };
    }

    /**
     * Get period name with specific dates
     */
    private String getPeriodName(String period, LocalDateTime date) {
        return switch (period.toLowerCase()) {
            case "day" -> "Ngày " + date.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            case "week" -> "Tuần " + date.format(java.time.format.DateTimeFormatter.ofPattern("ww/yyyy"));
            case "day_of_week" -> "Tuần " + date.format(java.time.format.DateTimeFormatter.ofPattern("ww/yyyy"));
            case "month" -> "Tháng " + date.format(java.time.format.DateTimeFormatter.ofPattern("MM/yyyy"));
            case "quarter" -> "Quý " + ((date.getMonthValue() - 1) / 3 + 1) + "/" + date.getYear();
            case "year" -> "Năm " + date.getYear();
            default -> "Kỳ " + date.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        };
    }

    /**
     * Simplify address for route display
     */
    private String simplifyAddress(String province) {
        if (province == null) return "N/A";
        // Remove "Tỉnh" or "Thành phố" prefix
        return province.replaceAll("^(Tỉnh|Thành phố)\\s+", "");
    }
}
