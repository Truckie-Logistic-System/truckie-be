package capstone_project.service.services.ai;

import capstone_project.entity.order.order.JourneyHistoryEntity;
import capstone_project.entity.order.order.JourneySegmentEntity;
import capstone_project.entity.order.order.OrderEntity;
import capstone_project.entity.order.order.OrderDetailEntity;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.repository.repositories.order.order.OrderRepository;
import capstone_project.repository.repositories.user.CustomerRepository;
import capstone_project.repository.repositories.vehicle.VehicleAssignmentRepository;
import capstone_project.repository.repositories.order.order.JourneyHistoryRepository;
import capstone_project.entity.user.customer.CustomerEntity;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for real-time order tracking information for AI chatbot
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderTrackingService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final VehicleAssignmentRepository vehicleAssignmentRepository;
    private final JourneyHistoryRepository journeyHistoryRepository;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final List<String> ACTIVE_STATUSES = List.of("IN_TRANSIT", "PICKING_UP", "DELIVERING");

    /**
     * Generate real-time tracking info for user's active orders
     */
    public String generateTrackingInfo(String userId) {
        log.info("🔍 DEBUG: generateTrackingInfo called with userId: {}", userId);
        
        if (userId == null || userId.isEmpty()) {
            return "⚠️ Vui lòng đăng nhập để xem thông tin theo dõi đơn hàng.\n\n";
        }

        try {
            UUID userUUID = UUID.fromString(userId);
            log.info("🔍 DEBUG: Parsed UUID: {}", userUUID);
            
            // Find orders by sender (customer) - First find customer by user ID, then get orders
            Optional<CustomerEntity> customerOpt = customerRepository.findByUserId(userUUID);
            if (customerOpt.isEmpty()) {
                log.warn("⚠️ No customer found for user_id: {}", userUUID);
                return "# 📦 THEO DÕI ĐƠN HÀNG\n\n" +
                       "⚠️ Không tìm thấy thông tin khách hàng.\n\n";
            }
            
            CustomerEntity customer = customerOpt.get();
            UUID customerId = customer.getId();
            log.info("📦 DEBUG: Found customer_id: {} for user_id: {}", customerId, userUUID);
            
            log.info("📦 DEBUG: Querying orders with sender_id: {}", customerId);
            List<OrderEntity> allOrders = orderRepository.findBySenderIdOrderByCreatedAtDesc(customerId);
            log.info("📦 DEBUG: Found {} total orders for customer {}", allOrders.size(), customerId);
            
            // Additional debug: Check if ANY orders exist in database
            List<OrderEntity> allOrdersInDb = orderRepository.findAll();
            log.info("📦 DEBUG: Total orders in database: {}", allOrdersInDb.size());
            if (!allOrdersInDb.isEmpty()) {
                allOrdersInDb.forEach(order -> 
                    log.info("📦 DEBUG: Order exists - sender_id: {}, order_code: {}", 
                        order.getSender() != null ? order.getSender().getId() : "null", 
                        order.getOrderCode())
                );
            }
            
            // Filter active orders that have orderDetails
            List<OrderEntity> activeOrders = allOrders.stream()
                    .filter(order -> order.getOrderDetailEntities() != null && !order.getOrderDetailEntities().isEmpty())
                    .filter(order -> order.getOrderDetailEntities().stream()
                            .anyMatch(od -> ACTIVE_STATUSES.contains(od.getStatus())))
                    .collect(Collectors.toList());
            
            log.info("🚛 DEBUG: Found {} active orders (in transit) for user {}", activeOrders.size(), userId);

            if (activeOrders.isEmpty()) {
                log.info("⚠️ No active orders in transit for user {}", userId);
                return "# 📦 THEO DÕI ĐƠN HÀNG\n\n" +
                       "✅ Hiện tại bạn không có đơn hàng nào đang vận chuyển.\n\n" +
                       "💡 Các đơn hàng đang chờ xử lý hoặc đã hoàn thành sẽ không hiển thị ở đây.\n\n";
            }

            StringBuilder info = new StringBuilder();
            info.append("# 🚛 THEO DÕI ĐƠN HÀNG REAL-TIME\n\n");
            info.append(String.format("Bạn có **%d đơn hàng** đang vận chuyển:\n\n", activeOrders.size()));

            for (OrderEntity order : activeOrders) {
                info.append(generateOrderTracking(order));
                info.append("\n---\n\n");
            }

            log.info("✅ Generated tracking info for user: {}, {} active orders", userId, activeOrders.size());
            return info.toString();

        } catch (IllegalArgumentException e) {
            log.error("❌ Invalid user ID format: {}", userId);
            return "⚠️ ID người dùng không hợp lệ.\n\n";
        } catch (Exception e) {
            log.error("❌ Error generating tracking info", e);
            return "⚠️ Không thể tải thông tin theo dõi. Vui lòng thử lại sau.\n\n";
        }
    }

    /**
     * Generate tracking info for single order
     */
    private String generateOrderTracking(OrderEntity order) {
        StringBuilder info = new StringBuilder();

        info.append(String.format("## 📦 Đơn Hàng: **%s**\n\n", order.getOrderCode()));
        info.append(String.format("**Trạng thái**: %s\n\n", translateStatus(order.getStatus())));

        // Get active order details
        List<OrderDetailEntity> activeDetails = order.getOrderDetailEntities().stream()
                .filter(od -> ACTIVE_STATUSES.contains(od.getStatus()))
                .collect(Collectors.toList());

        if (activeDetails.isEmpty()) {
            info.append("⏳ Đơn hàng chưa bắt đầu vận chuyển.\n");
            return info.toString();
        }

        // Process each order detail
        for (int i = 0; i < activeDetails.size(); i++) {
            OrderDetailEntity detail = activeDetails.get(i);
            if (activeDetails.size() > 1) {
                info.append(String.format("### Chuyến %d/%d\n\n", i + 1, activeDetails.size()));
            }

            info.append(generateDetailTracking(detail));
        }

        return info.toString();
    }

    /**
     * Generate tracking for order detail
     */
    private String generateDetailTracking(OrderDetailEntity detail) {
        StringBuilder info = new StringBuilder();

        VehicleAssignmentEntity assignment = detail.getVehicleAssignmentEntity();
        
        if (assignment == null) {
            info.append("⏳ Chưa phân công xe.\n\n");
            return info.toString();
        }

        // Vehicle info
        if (assignment.getVehicleEntity() != null) {
            info.append(String.format("🚛 **Xe**: %s (%s)\n",
                    assignment.getVehicleEntity().getLicensePlateNumber(),
                    assignment.getVehicleEntity().getVehicleTypeEntity() != null 
                        ? assignment.getVehicleEntity().getVehicleTypeEntity().getVehicleTypeName()
                        : "N/A"
            ));
        }

        // Driver info
        if (assignment.getDriver1() != null && assignment.getDriver1().getUser() != null) {
            info.append(String.format("👨‍✈️ **Tài xế**: %s (%s)\n",
                    assignment.getDriver1().getUser().getFullName(),
                    assignment.getDriver1().getUser().getPhoneNumber()
            ));
        }

        // ETA from order detail
        if (detail.getEstimatedEndTime() != null) {
            LocalDateTime eta = detail.getEstimatedEndTime();
            String etaStr = eta.format(TIME_FORMATTER);
            info.append(String.format("⏱️ **Dự kiến đến**: %s\n", etaStr));
        }

        info.append(String.format("📍 **Trạng thái**: %s\n", translateStatus(detail.getStatus())));

        // Journey progress
        try {
            List<JourneyHistoryEntity> journeys = journeyHistoryRepository
                    .findByVehicleAssignment_IdOrderByCreatedAtDesc(assignment.getId());

            if (!journeys.isEmpty()) {
                JourneyHistoryEntity latestJourney = journeys.get(0);
                info.append(generateJourneyProgress(latestJourney));
            }
        } catch (Exception e) {
            log.warn("⚠️ Could not load journey info", e);
        }

        info.append("\n");
        return info.toString();
    }

    /**
     * Generate journey progress
     */
    private String generateJourneyProgress(JourneyHistoryEntity journey) {
        StringBuilder info = new StringBuilder();

        // Get segments from journey entity
        List<JourneySegmentEntity> segments = journey.getJourneySegments();

        if (segments == null || segments.isEmpty()) {
            return "";
        }

        // Find active segment (status = ACTIVE)
        JourneySegmentEntity activeSegment = segments.stream()
                .filter(s -> "ACTIVE".equals(s.getStatus()))
                .findFirst()
                .orElse(null);

        if (activeSegment == null) {
            // Get most recent segment
            activeSegment = segments.stream()
                    .max((a, b) -> {
                        Integer orderA = a.getSegmentOrder() != null ? a.getSegmentOrder() : 0;
                        Integer orderB = b.getSegmentOrder() != null ? b.getSegmentOrder() : 0;
                        return orderA.compareTo(orderB);
                    })
                    .orElse(null);
        }

        if (activeSegment == null) {
            return "";
        }

        // Calculate progress
        long completedSegments = segments.stream()
                .filter(s -> "COMPLETED".equals(s.getStatus()))
                .count();
        int totalSegments = segments.size();
        int progress = totalSegments > 0 ? (int) ((completedSegments * 100) / totalSegments) : 0;

        info.append(String.format("\n**📍 Tiến độ**: %d%% (%d/%d điểm)\n", 
                progress, completedSegments, totalSegments));

        // Current route
        if (activeSegment.getStartPointName() != null && activeSegment.getEndPointName() != null) {
            info.append(String.format("**🛣️ Tuyến đường**: %s → %s\n",
                    activeSegment.getStartPointName(),
                    activeSegment.getEndPointName()
            ));
        }

        return info.toString();
    }

    /**
     * Translate status to Vietnamese
     */
    private String translateStatus(String status) {
        if (status == null) return "Không rõ";
        
        return switch (status) {
            case "PENDING_QUOTE" -> "Chờ báo giá";
            case "PENDING_SIGNATURE" -> "Chờ ký hợp đồng";
            case "PENDING_DEPOSIT" -> "Chờ đặt cọc";
            case "PENDING_ASSIGNMENT" -> "Chờ phân công";
            case "ASSIGNED" -> "Đã phân công";
            case "PICKING_UP" -> "Đang lấy hàng";
            case "IN_TRANSIT" -> "Đang vận chuyển";
            case "DELIVERING" -> "Đang giao hàng";
            case "COMPLETED" -> "Đã hoàn thành";
            case "CANCELLED" -> "Đã hủy";
            default -> status;
        };
    }
}
