package capstone_project.service.services.notification;

import capstone_project.dtos.request.notification.CreateNotificationRequest;
import capstone_project.entity.auth.UserEntity;
import capstone_project.entity.order.order.OrderDetailEntity;
import capstone_project.common.enums.NotificationTypeEnum;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Helper class để build notification content cho các scenarios khác nhau
 * Version 2.0 - Parameter-based approach để avoid entity dependency issues
 * 
 * Tất cả notification content đều bằng tiếng Việt
 */
public class NotificationBuilder {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    
    /**
     * Tạo package metadata với thông tin chi tiết cho email
     */
    private static List<Map<String, Object>> createPackageMetadata(List<OrderDetailEntity> orderDetails) {
        List<Map<String, Object>> packages = new ArrayList<>();
        for (OrderDetailEntity od : orderDetails) {
            Map<String, Object> packageInfo = new HashMap<>();
            packageInfo.put("trackingCode", od.getTrackingCode());
            packageInfo.put("description", od.getDescription());
            if (od.getWeightBaseUnit() != null) {
                packageInfo.put("weightBaseUnit", od.getWeightBaseUnit().doubleValue());
                packageInfo.put("weight", String.format("%.1f %s", od.getWeightBaseUnit(), od.getUnit() != null ? od.getUnit() : "kg"));
            } else {
                packageInfo.put("weight", "N/A");
                packageInfo.put("weightBaseUnit", 0.0);
            }
            packageInfo.put("unit", od.getUnit() != null ? od.getUnit() : "kg");
            packages.add(packageInfo);
        }
        return packages;
    }
    
    /**
     * Format package list cho description text
     */
    private static String formatPackageList(List<OrderDetailEntity> orderDetails) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < orderDetails.size(); i++) {
            OrderDetailEntity od = orderDetails.get(i);
            String weight = od.getWeightBaseUnit() != null ? 
                String.format("%.1f %s", od.getWeightBaseUnit(), od.getUnit() != null ? od.getUnit() : "kg") : "N/A";
            sb.append(String.format("• %s - %s (%s)", 
                od.getTrackingCode(), 
                od.getDescription(), 
                weight));
            if (i < orderDetails.size() - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }
    
    // ============= ORDER LIFECYCLE NOTIFICATIONS =============
    
    /**
     * ORDER_CREATED - Đơn hàng đã tạo thành công
     * Version with full order details - creates separate metadata for each package
     */
    public static CreateNotificationRequest buildOrderCreated(
        UUID userId,
        String orderCode,
        List<OrderDetailEntity> orderDetails,
        UUID orderId
    ) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("orderCode", orderCode);
        metadata.put("packageCount", orderDetails.size());
        
        // Calculate total weight
        double totalWeight = orderDetails.stream()
            .filter(detail -> detail.getWeightBaseUnit() != null)
            .mapToDouble(detail -> detail.getWeightBaseUnit().doubleValue())
            .sum();
        
        String weightUnit = orderDetails.stream()
            .filter(detail -> detail.getUnit() != null && !detail.getUnit().isEmpty())
            .map(OrderDetailEntity::getUnit)
            .findFirst()
            .orElse("kg");
        
        metadata.put("totalWeight", String.format("%.2f %s", totalWeight, weightUnit));
        
        // Add packages as separate items in metadata
        List<Map<String, Object>> packages = createPackageMetadata(orderDetails);
        metadata.put("packages", packages);
        
        return CreateNotificationRequest.builder()
            .userId(userId)
            .recipientRole("CUSTOMER")
            .title(String.format("Đơn hàng %s đã tạo thành công", orderCode))
            .description("Đơn hàng của bạn đã được tạo và đang chờ xử lý. Vui lòng vào trang chi tiết đơn hàng để xem đề xuất phương tiện vận chuyển.")
            .notificationType(NotificationTypeEnum.ORDER_CREATED)
            .relatedOrderId(orderId)
            .metadata(metadata)
            .build();
    }
    
    /**
     * ORDER_CREATED - Đơn hàng đã tạo thành công (Legacy - simplified version)
     * @deprecated Use buildOrderCreated with List<OrderDetailEntity> for full package details
     */
    @Deprecated
    public static CreateNotificationRequest buildOrderCreated(
        UUID userId,
        String orderCode,
        int packageCount,
        double totalWeight,
        String weightUnit,
        String packageDescription,
        UUID orderId
    ) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("orderCode", orderCode);
        metadata.put("packageCount", packageCount);
        metadata.put("totalWeight", String.format("%.2f %s", totalWeight, weightUnit != null ? weightUnit : "kg"));
        if (packageDescription != null && !packageDescription.trim().isEmpty()) {
            metadata.put("packageDescription", packageDescription);
        }
        
        return CreateNotificationRequest.builder()
            .userId(userId)
            .recipientRole("CUSTOMER")
            .title(String.format("Đơn hàng %s đã tạo thành công", orderCode))
            .description("Đơn hàng của bạn đã được tạo và đang chờ xử lý. Vui lòng vào trang chi tiết đơn hàng để xem đề xuất phương tiện vận chuyển.")
            .notificationType(NotificationTypeEnum.ORDER_CREATED)
            .relatedOrderId(orderId)
            .metadata(metadata)
            .build();
    }
    
    /**
     * CONTRACT_READY - Hợp đồng đã sẵn sàng để ký
     */
    public static CreateNotificationRequest buildContractReady(
        UUID userId,
        String orderCode,
        String contractCode,
        double depositAmount,
        double totalAmount,
        LocalDateTime signDeadline,
        LocalDateTime depositDeadline,
        UUID orderId,
        UUID contractId
    ) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("contractCode", contractCode);
        metadata.put("orderCode", orderCode);
        metadata.put("depositAmount", String.format("%,.0f VNĐ", depositAmount));
        metadata.put("totalAmount", String.format("%,.0f VNĐ", totalAmount));
        if (signDeadline != null) {
            metadata.put("signDeadline", signDeadline.format(DATE_FORMATTER));
        }
        if (depositDeadline != null) {
            metadata.put("depositDeadline", depositDeadline.format(DATE_FORMATTER));
        }
        
        String description = String.format(
            "Hợp đồng vận chuyển cho đơn hàng %s đã được tạo. Vui lòng ký hợp đồng và thanh toán tiền cọc %,.0f VNĐ trước %s.%n⚠️ Lưu ý: Đơn hàng sẽ tự động hủy nếu quá thời hạn.",
            orderCode,
            depositAmount,
            depositDeadline != null ? depositDeadline.format(DATE_FORMATTER) : "hạn thanh toán"
        );
        
        return CreateNotificationRequest.builder()
            .userId(userId)
            .recipientRole("CUSTOMER")
            .title(String.format("Hợp đồng %s đã sẵn sàng để ký", contractCode))
            .description(description)
            .notificationType(NotificationTypeEnum.CONTRACT_READY)
            .relatedOrderId(orderId)
            .relatedContractId(contractId)
            .metadata(metadata)
            .build();
    }
    
    /**
     * DRIVER_ASSIGNED - Đã phân công tài xế
     */
    public static CreateNotificationRequest buildDriverAssigned(
        UUID userId,
        String orderCode,
        String driverName,
        String driverPhone,
        String vehiclePlate,
        String vehicleType,
        double remainingAmount,
        LocalDateTime paymentDeadline,
        LocalDateTime estimatedPickupDate,
        UUID orderId,
        UUID vehicleAssignmentId
    ) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("orderCode", orderCode);
        metadata.put("driverName", driverName);
        metadata.put("driverPhone", driverPhone);
        metadata.put("vehiclePlate", vehiclePlate);
        metadata.put("vehicleType", vehicleType);
        metadata.put("remainingAmount", String.format("%,.0f VNĐ", remainingAmount));
        if (paymentDeadline != null) {
            metadata.put("paymentDeadline", paymentDeadline.format(DATE_FORMATTER));
        }
        if (estimatedPickupDate != null) {
            metadata.put("estimatedPickupDate", estimatedPickupDate.format(DATE_FORMATTER));
        }
        
        String description = String.format(
            "Tài xế %s (%s) đã được phân công vận chuyển đơn hàng của bạn. Vui lòng thanh toán số tiền còn lại %,.0f VNĐ trước %s.",
            driverName,
            vehiclePlate,
            remainingAmount,
            paymentDeadline != null ? paymentDeadline.format(DATE_FORMATTER) : "hạn thanh toán"
        );
        
        return CreateNotificationRequest.builder()
            .userId(userId)
            .recipientRole("CUSTOMER")
            .title(String.format("Đã phân công tài xế cho đơn hàng %s", orderCode))
            .description(description)
            .notificationType(NotificationTypeEnum.DRIVER_ASSIGNED)
            .relatedOrderId(orderId)
            .relatedVehicleAssignmentId(vehicleAssignmentId)
            .metadata(metadata)
            .build();
    }
    
    /**
     * PAYMENT_FULL_SUCCESS - Thanh toán đủ thành công (cho Customer)
     */
    public static CreateNotificationRequest buildPaymentFullSuccess(
        UUID userId,
        String orderCode,
        String contractCode,
        double totalAmount,
        UUID orderId,
        UUID contractId
    ) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("contractCode", contractCode);
        metadata.put("orderCode", orderCode);
        metadata.put("totalAmount", String.format("%,.0f VNĐ", totalAmount));
        
        return CreateNotificationRequest.builder()
            .userId(userId)
            .recipientRole("CUSTOMER")
            .title(String.format("Thanh toán đủ thành công - Đơn %s", orderCode))
            .description("Bạn đã thanh toán đủ. Tài xế sẽ bắt đầu lấy hàng theo lịch hẹn.")
            .notificationType(NotificationTypeEnum.PAYMENT_FULL_SUCCESS)
            .relatedOrderId(orderId)
            .relatedContractId(contractId)
            .metadata(metadata)
            .build();
    }
    
    /**
     * PAYMENT_RECEIVED - Khách đã thanh toán đủ (cho Driver)
     */
    public static CreateNotificationRequest buildPaymentReceived(
        UUID userId,
        String orderCode,
        double totalAmount,
        String customerName,
        String customerPhone,
        UUID orderId,
        UUID contractId
    ) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("orderCode", orderCode);
        metadata.put("totalAmount", String.format("%,.0f VNĐ", totalAmount));
        metadata.put("customerName", customerName);
        metadata.put("customerPhone", customerPhone);
        
        String description = String.format(
            "Khách hàng đã thanh toán đủ số tiền %,.0f VNĐ cho đơn hàng %s. Bạn có thể bắt đầu lấy hàng vào ngày đã hẹn.",
            totalAmount,
            orderCode
        );
        
        return CreateNotificationRequest.builder()
            .userId(userId)
            .recipientRole("DRIVER")
            .title(String.format("Khách đã thanh toán đủ - Đơn %s", orderCode))
            .description(description)
            .notificationType(NotificationTypeEnum.PAYMENT_RECEIVED)
            .relatedOrderId(orderId)
            .relatedContractId(contractId)
            .metadata(metadata)
            .build();
    }
    
    /**
     * NEW_ORDER_ASSIGNED - Đơn hàng mới được phân công (cho Driver)
     */
    public static CreateNotificationRequest buildNewOrderAssigned(
        UUID userId,
        String orderCode,
        int packageCount,
        double totalWeight,
        String weightUnit,
        String packageDescription,
        String vehicleType,
        LocalDateTime pickupDate,
        String pickupLocation,
        String deliveryLocation,
        UUID orderId,
        UUID vehicleAssignmentId
    ) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("orderCode", orderCode);
        metadata.put("packageCount", packageCount);
        metadata.put("totalWeight", String.format("%.2f %s", totalWeight, weightUnit != null ? weightUnit : "kg"));
        if (packageDescription != null && !packageDescription.trim().isEmpty()) {
            metadata.put("packageDescription", packageDescription);
        }
        metadata.put("vehicleType", vehicleType);
        if (pickupDate != null) {
            metadata.put("pickupDate", pickupDate.format(DATE_FORMATTER));
        }
        metadata.put("pickupLocation", pickupLocation);
        metadata.put("deliveryLocation", deliveryLocation);
        
        String description = String.format(
            "Bạn được phân công vận chuyển đơn hàng %s với %d kiện hàng. Ngày lấy hàng dự kiến: %s.%n📍 Lấy: %s%n📍 Giao: %s",
            orderCode,
            packageCount,
            pickupDate != null ? pickupDate.format(DATE_FORMATTER) : "Chưa xác định",
            pickupLocation,
            deliveryLocation
        );
        
        return CreateNotificationRequest.builder()
            .userId(userId)
            .recipientRole("DRIVER")
            .title(String.format("Đơn hàng mới %s - %s", orderCode, vehicleType))
            .description(description)
            .notificationType(NotificationTypeEnum.NEW_ORDER_ASSIGNED)
            .relatedOrderId(orderId)
            .relatedVehicleAssignmentId(vehicleAssignmentId)
            .metadata(metadata)
            .build();
    }
    
    // ============= DELIVERY TRACKING NOTIFICATIONS =============
    
    /**
     * PICKING_UP_STARTED - Tài xế bắt đầu lấy hàng (cho Customer - Email: YES)
     */
    public static CreateNotificationRequest buildPickingUpStarted(
        UUID userId,
        String orderCode,
        String driverName,
        String driverPhone,
        String vehiclePlate,
        int packageCount,
        UUID orderId,
        UUID vehicleAssignmentId
    ) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("orderCode", orderCode);
        metadata.put("driverName", driverName);
        metadata.put("driverPhone", driverPhone);
        metadata.put("vehiclePlate", vehiclePlate);
        metadata.put("packageCount", packageCount);
        
        String description = String.format(
            "Tài xế %s (%s) đang trên đường đến lấy %d kiện hàng của bạn. Vui lòng vào trang đơn hàng để theo dõi vị trí thời gian thực.",
            driverName,
            vehiclePlate,
            packageCount
        );
        
        return CreateNotificationRequest.builder()
            .userId(userId)
            .recipientRole("CUSTOMER")
            .title(String.format("Tài xế bắt đầu lấy hàng - Đơn %s", orderCode))
            .description(description)
            .notificationType(NotificationTypeEnum.PICKING_UP_STARTED)
            .relatedOrderId(orderId)
            .relatedVehicleAssignmentId(vehicleAssignmentId)
            .metadata(metadata)
            .build();
    }
    
    /**
     * DELIVERY_STARTED - Đang vận chuyển hàng (cho Customer - Email: NO)
     */
    public static CreateNotificationRequest buildDeliveryStarted(
        UUID userId,
        String orderCode,
        String driverName,
        String vehiclePlate,
        int packageCount,
        String deliveryLocation,
        UUID orderId,
        UUID vehicleAssignmentId
    ) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("orderCode", orderCode);
        metadata.put("driverName", driverName);
        metadata.put("vehiclePlate", vehiclePlate);
        metadata.put("packageCount", packageCount);
        metadata.put("deliveryLocation", deliveryLocation);
        
        String description = String.format(
            "Tài xế %s đang vận chuyển %d kiện hàng của bạn đến %s. Bạn có thể theo dõi vị trí thời gian thực trên trang đơn hàng.",
            driverName,
            packageCount,
            deliveryLocation
        );
        
        return CreateNotificationRequest.builder()
            .userId(userId)
            .recipientRole("CUSTOMER")
            .title(String.format("Đang vận chuyển - Đơn %s", orderCode))
            .description(description)
            .notificationType(NotificationTypeEnum.DELIVERY_STARTED)
            .relatedOrderId(orderId)
            .relatedVehicleAssignmentId(vehicleAssignmentId)
            .metadata(metadata)
            .build();
    }
    
    /**
     * DELIVERY_IN_PROGRESS - Sắp giao hàng (cho Customer - Email: NO)
     */
    public static CreateNotificationRequest buildDeliveryInProgress(
        UUID userId,
        String orderCode,
        String driverName,
        String driverPhone,
        int packageCount,
        String deliveryLocation,
        UUID orderId,
        UUID vehicleAssignmentId
    ) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("orderCode", orderCode);
        metadata.put("driverName", driverName);
        metadata.put("driverPhone", driverPhone);
        metadata.put("packageCount", packageCount);
        metadata.put("deliveryLocation", deliveryLocation);
        
        String description = String.format(
            "Tài xế %s sắp đến điểm giao hàng với %d kiện hàng. Vui lòng chuẩn bị nhận hàng.",
            driverName,
            packageCount
        );
        
        return CreateNotificationRequest.builder()
            .userId(userId)
            .recipientRole("CUSTOMER")
            .title(String.format("Sắp giao hàng - Đơn %s", orderCode))
            .description(description)
            .notificationType(NotificationTypeEnum.DELIVERY_IN_PROGRESS)
            .relatedOrderId(orderId)
            .relatedVehicleAssignmentId(vehicleAssignmentId)
            .metadata(metadata)
            .build();
    }
    
    /**
     * DELIVERY_COMPLETED - Giao hàng thành công (cho Customer)
     * @param allPackagesDelivered true nếu ALL packages đã giao, false nếu chỉ SOME
     */
    public static CreateNotificationRequest buildDeliveryCompleted(
        UUID userId,
        String orderCode,
        int deliveredCount,
        int totalPackageCount,
        String deliveryLocation,
        String receiverName,
        List<OrderDetailEntity> deliveredPackages,
        UUID orderId,
        List<UUID> deliveredOrderDetailIds,
        UUID vehicleAssignmentId,
        boolean allPackagesDelivered
    ) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("orderCode", orderCode);
        metadata.put("deliveredCount", deliveredCount);
        metadata.put("totalPackageCount", totalPackageCount);
        metadata.put("deliveryLocation", deliveryLocation);
        metadata.put("receiverName", receiverName);
        metadata.put("allPackagesDelivered", allPackagesDelivered);
        
        // Thêm thông tin chi tiết về các kiện hàng đã giao
        List<Map<String, Object>> packageDetails = createPackageMetadata(deliveredPackages);
        metadata.put("deliveredPackages", packageDetails);
        
        String title;
        String description;
        
        if (allPackagesDelivered) {
            title = String.format("Đơn hàng %s đã giao thành công", orderCode);
            description = String.format(
                "Tất cả %d kiện hàng của đơn %s đã được giao thành công đến %s. Cảm ơn bạn đã sử dụng dịch vụ!\n\n" +
                "📦 CHI TIẾT KIỆN HÀNG ĐÃ GIAO:\n" +
                "%s",
                deliveredCount,
                orderCode,
                deliveryLocation,
                formatPackageList(deliveredPackages)
            );
        } else {
            title = String.format("%d/%d kiện đơn %s đã giao", deliveredCount, totalPackageCount, orderCode);
            description = String.format(
                "%d kiện hàng của đơn %s đã được giao đến %s. Các kiện hàng còn lại đang được vận chuyển.\n\n" +
                "📦 CHI TIẾT KIỆN HÀNG ĐÃ GIAO:\n" +
                "%s",
                deliveredCount,
                orderCode,
                deliveryLocation,
                formatPackageList(deliveredPackages)
            );
        }
        
        return CreateNotificationRequest.builder()
            .userId(userId)
            .recipientRole("CUSTOMER")
            .title(title)
            .description(description)
            .notificationType(NotificationTypeEnum.DELIVERY_COMPLETED)
            .relatedOrderId(orderId)
            .relatedOrderDetailIds(deliveredOrderDetailIds)
            .relatedVehicleAssignmentId(vehicleAssignmentId)
            .metadata(metadata)
            .build();
    }
    
    /**
     * RETURN_STARTED - Cần thanh toán cước trả hàng (cho Customer - Email: YES - ACTION)
     */
    public static CreateNotificationRequest buildReturnStarted(
        UUID userId,
        String orderCode,
        int returnCount,
        int totalPackageCount,
        double returnShippingFee,
        LocalDateTime paymentDeadline,
        UUID orderId,
        UUID issueId,
        List<UUID> returnOrderDetailIds
    ) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("orderCode", orderCode);
        metadata.put("returnCount", returnCount);
        metadata.put("totalPackageCount", totalPackageCount);
        metadata.put("returnShippingFee", String.format("%,.0f VNĐ", returnShippingFee));
        if (paymentDeadline != null) {
            metadata.put("paymentDeadline", paymentDeadline.format(DATE_FORMATTER));
        }
        
        String title;
        String description;
        
        if (returnCount == totalPackageCount) {
            title = String.format("Đơn hàng %s cần thanh toán cước trả", orderCode);
        } else {
            title = String.format("%d kiện đơn %s cần thanh toán cước trả", returnCount, orderCode);
        }
        
        description = String.format(
            "%d kiện hàng cần được trả lại. Vui lòng thanh toán cước trả hàng %,.0f VNĐ trước %s để tài xế tiến hành trả hàng.%n⚠️ Hàng sẽ bị hủy nếu quá hạn thanh toán.",
            returnCount,
            returnShippingFee,
            paymentDeadline != null ? paymentDeadline.format(DATE_FORMATTER) : "hạn thanh toán"
        );
        
        return CreateNotificationRequest.builder()
            .userId(userId)
            .recipientRole("CUSTOMER")
            .title(title)
            .description(description)
            .notificationType(NotificationTypeEnum.RETURN_STARTED)
            .relatedOrderId(orderId)
            .relatedIssueId(issueId)
            .relatedOrderDetailIds(returnOrderDetailIds)
            .metadata(metadata)
            .build();
    }
    
    /**
     * RETURN_COMPLETED - Trả hàng thành công (cho Customer)
     */
    public static CreateNotificationRequest buildReturnCompleted(
        UUID userId,
        String orderCode,
        int returnedCount,
        int totalPackageCount,
        String pickupLocation,
        List<OrderDetailEntity> returnedPackages,
        UUID orderId,
        List<UUID> returnedOrderDetailIds,
        UUID vehicleAssignmentId,
        boolean allPackagesReturned
    ) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("orderCode", orderCode);
        metadata.put("returnedCount", returnedCount);
        metadata.put("totalPackageCount", totalPackageCount);
        metadata.put("pickupLocation", pickupLocation);
        metadata.put("allPackagesReturned", allPackagesReturned);
        
        // Thêm thông tin chi tiết về các kiện hàng đã trả
        List<Map<String, Object>> packageDetails = createPackageMetadata(returnedPackages);
        metadata.put("returnedPackages", packageDetails);
        
        String title;
        String description;
        
        if (allPackagesReturned) {
            title = String.format("Đơn hàng %s đã trả thành công", orderCode);
            description = String.format(
                "Tất cả %d kiện hàng của đơn %s đã được trả về %s thành công.\n\n" +
                "📦 CHI TIẾT KIỆN HÀNG ĐÃ TRẢ:\n" +
                "%s",
                returnedCount,
                orderCode,
                pickupLocation,
                formatPackageList(returnedPackages)
            );
        } else {
            title = String.format("%d/%d kiện đơn %s đã trả", returnedCount, totalPackageCount, orderCode);
            description = String.format(
                "%d kiện hàng của đơn %s đã được trả về %s.\n\n" +
                "📦 CHI TIẾT KIỆN HÀNG ĐÃ TRẢ:\n" +
                "%s",
                returnedCount,
                orderCode,
                pickupLocation,
                formatPackageList(returnedPackages)
            );
        }
        
        return CreateNotificationRequest.builder()
            .userId(userId)
            .recipientRole("CUSTOMER")
            .title(title)
            .description(description)
            .notificationType(NotificationTypeEnum.RETURN_COMPLETED)
            .relatedOrderId(orderId)
            .relatedOrderDetailIds(returnedOrderDetailIds)
            .relatedVehicleAssignmentId(vehicleAssignmentId)
            .metadata(metadata)
            .build();
    }
    
    /**
     * COMPENSATION_PROCESSED - Bồi thường đã xử lý (cho Customer - Email: YES)
     */
    public static CreateNotificationRequest buildCompensationProcessed(
        UUID userId,
        String orderCode,
        int compensatedCount,
        int totalPackageCount,
        double compensationAmount,
        String refundMethod,
        UUID orderId,
        UUID issueId,
        List<UUID> compensatedOrderDetailIds
    ) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("orderCode", orderCode);
        metadata.put("compensatedCount", compensatedCount);
        metadata.put("totalPackageCount", totalPackageCount);
        metadata.put("compensationAmount", String.format("%,.0f VNĐ", compensationAmount));
        metadata.put("refundMethod", refundMethod);
        
        String description = String.format(
            "%d kiện hàng của đơn %s đã được đền bù với số tiền %,.0f VNĐ. Số tiền sẽ được hoàn lại qua %s trong 3-5 ngày làm việc.",
            compensatedCount,
            orderCode,
            compensationAmount,
            refundMethod
        );
        
        return CreateNotificationRequest.builder()
            .userId(userId)
            .recipientRole("CUSTOMER")
            .title(String.format("Đền bù %d kiện - Đơn %s", compensatedCount, orderCode))
            .description(description)
            .notificationType(NotificationTypeEnum.COMPENSATION_PROCESSED)
            .relatedOrderId(orderId)
            .relatedIssueId(issueId)
            .relatedOrderDetailIds(compensatedOrderDetailIds)
            .metadata(metadata)
            .build();
    }
    
    /**
     * ORDER_CANCELLED - Đơn hàng/kiện hàng bị hủy (cho Customer - Email: YES)
     */
    public static CreateNotificationRequest buildOrderCancelledMultiTrip(
        UUID userId,
        String orderCode,
        int cancelledCount,
        int totalPackageCount,
        String cancelReason,
        UUID orderId,
        List<UUID> cancelledOrderDetailIds,
        boolean allPackagesCancelled
    ) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("orderCode", orderCode);
        metadata.put("cancelledCount", cancelledCount);
        metadata.put("totalPackageCount", totalPackageCount);
        metadata.put("cancelReason", cancelReason);
        metadata.put("allPackagesCancelled", allPackagesCancelled);
        
        String title;
        String description;
        
        if (allPackagesCancelled) {
            title = String.format("Đơn hàng %s đã bị hủy", orderCode);
            description = String.format(
                "Đơn hàng %s đã bị hủy do: %s",
                orderCode,
                cancelReason
            );
        } else {
            title = String.format("%d kiện đơn %s đã bị hủy", cancelledCount, orderCode);
            description = String.format(
                "%d/%d kiện hàng của đơn %s đã bị hủy do: %s. Các kiện hàng còn lại vẫn đang được xử lý.",
                cancelledCount,
                totalPackageCount,
                orderCode,
                cancelReason
            );
        }
        
        return CreateNotificationRequest.builder()
            .userId(userId)
            .recipientRole("CUSTOMER")
            .title(title)
            .description(description)
            .notificationType(NotificationTypeEnum.ORDER_CANCELLED)
            .relatedOrderId(orderId)
            .relatedOrderDetailIds(cancelledOrderDetailIds)
            .metadata(metadata)
            .build();
    }
    
    // ============= STAFF NOTIFICATION TEMPLATES =============
    
    /**
     * STAFF_ORDER_CREATED - Đơn hàng mới được tạo (cho Staff)
     */
    public static CreateNotificationRequest buildStaffOrderCreated(
        UUID staffUserId,
        String orderCode,
        String customerName,
        String customerPhone,
        int packageCount,
        double totalWeight,
        String weightUnit,
        UUID orderId
    ) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("orderCode", orderCode);
        metadata.put("customerName", customerName);
        metadata.put("customerPhone", customerPhone);
        metadata.put("packageCount", packageCount);
        String formattedWeight = String.format("%.2f %s", totalWeight, weightUnit != null ? weightUnit : "kg");
        metadata.put("totalWeight", formattedWeight);
        
        String description = String.format(
            "Khách hàng %s (%s) vừa tạo đơn hàng mới với %d kiện hàng, tổng %s.",
            customerName,
            customerPhone,
            packageCount,
            formattedWeight
        );
        
        return CreateNotificationRequest.builder()
            .userId(staffUserId)
            .recipientRole("STAFF")
            .title(String.format("Đơn hàng mới: %s", orderCode))
            .description(description)
            .notificationType(NotificationTypeEnum.STAFF_ORDER_CREATED)
            .relatedOrderId(orderId)
            .metadata(metadata)
            .build();
    }
    
    /**
     * STAFF_ORDER_PROCESSING - Đơn hàng cần tạo hợp đồng (cho Staff)
     */
    public static CreateNotificationRequest buildStaffOrderProcessing(
        UUID staffUserId,
        String orderCode,
        String customerName,
        String customerPhone,
        int packageCount,
        UUID orderId
    ) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("orderCode", orderCode);
        metadata.put("customerName", customerName);
        metadata.put("customerPhone", customerPhone);
        metadata.put("packageCount", packageCount);
        
        String description = String.format(
            "Khách hàng %s đã đồng ý với đề xuất xe hàng. Vui lòng tạo hợp đồng cho đơn %s.",
            customerName,
            orderCode
        );
        
        return CreateNotificationRequest.builder()
            .userId(staffUserId)
            .recipientRole("STAFF")
            .title(String.format("Cần tạo hợp đồng - Đơn %s", orderCode))
            .description(description)
            .notificationType(NotificationTypeEnum.STAFF_ORDER_PROCESSING)
            .relatedOrderId(orderId)
            .metadata(metadata)
            .build();
    }
    
    /**
     * STAFF_CONTRACT_SIGNED - Hợp đồng đã được ký (cho Staff)
     */
    public static CreateNotificationRequest buildStaffContractSigned(
        UUID staffUserId,
        String orderCode,
        String contractCode,
        String customerName,
        UUID orderId,
        UUID contractId
    ) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("orderCode", orderCode);
        metadata.put("contractCode", contractCode);
        metadata.put("customerName", customerName);
        
        String description = String.format(
            "Khách hàng %s đã ký hợp đồng %s cho đơn %s.",
            customerName,
            contractCode,
            orderCode
        );
        
        return CreateNotificationRequest.builder()
            .userId(staffUserId)
            .recipientRole("STAFF")
            .title(String.format("HĐ %s đã được ký", contractCode))
            .description(description)
            .notificationType(NotificationTypeEnum.STAFF_CONTRACT_SIGNED)
            .relatedOrderId(orderId)
            .relatedContractId(contractId)
            .metadata(metadata)
            .build();
    }
    
    /**
     * STAFF_DEPOSIT_RECEIVED - Đã nhận cọc - cần lên lộ trình (cho Staff)
     */
    public static CreateNotificationRequest buildStaffDepositReceived(
        UUID staffUserId,
        String orderCode,
        double depositAmount,
        String customerName,
        int packageCount,
        UUID orderId
    ) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("orderCode", orderCode);
        metadata.put("depositAmount", String.format("%,.0f VNĐ", depositAmount));
        metadata.put("customerName", customerName);
        metadata.put("packageCount", packageCount);
        
        String description = String.format(
            "Đơn %s đã thanh toán cọc %,.0f VNĐ. Cần lên lộ trình và phân công tài xế cho %d kiện hàng.",
            orderCode,
            depositAmount,
            packageCount
        );
        
        return CreateNotificationRequest.builder()
            .userId(staffUserId)
            .recipientRole("STAFF")
            .title(String.format("Cần lên lộ trình - Đơn %s", orderCode))
            .description(description)
            .notificationType(NotificationTypeEnum.STAFF_DEPOSIT_RECEIVED)
            .relatedOrderId(orderId)
            .metadata(metadata)
            .build();
    }
    
    /**
     * STAFF_FULL_PAYMENT - Đã thanh toán đủ (cho Staff)
     */
    public static CreateNotificationRequest buildStaffFullPayment(
        UUID staffUserId,
        String orderCode,
        double totalAmount,
        String customerName,
        UUID orderId
    ) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("orderCode", orderCode);
        metadata.put("totalAmount", String.format("%,.0f VNĐ", totalAmount));
        metadata.put("customerName", customerName);
        
        String description = String.format(
            "Khách hàng %s đã thanh toán đủ %,.0f VNĐ cho đơn %s. Tài xế có thể bắt đầu vận chuyển.",
            customerName,
            totalAmount,
            orderCode
        );
        
        return CreateNotificationRequest.builder()
            .userId(staffUserId)
            .recipientRole("STAFF")
            .title(String.format("Thanh toán đủ - Đơn %s", orderCode))
            .description(description)
            .notificationType(NotificationTypeEnum.STAFF_FULL_PAYMENT)
            .relatedOrderId(orderId)
            .metadata(metadata)
            .build();
    }
    
    /**
     * STAFF_RETURN_PAYMENT - Cước trả hàng đã thanh toán (cho Staff)
     */
    public static CreateNotificationRequest buildStaffReturnPayment(
        UUID staffUserId,
        String orderCode,
        double returnShippingFee,
        String customerName,
        int returnCount,
        UUID orderId,
        UUID issueId
    ) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("orderCode", orderCode);
        metadata.put("returnShippingFee", String.format("%,.0f VNĐ", returnShippingFee));
        metadata.put("customerName", customerName);
        metadata.put("returnCount", returnCount);
        
        String description = String.format(
            "Khách %s đã thanh toán cước trả hàng %,.0f VNĐ cho %d kiện của đơn %s. Tài xế sẽ tiến hành trả hàng.",
            customerName,
            returnShippingFee,
            returnCount,
            orderCode
        );
        
        return CreateNotificationRequest.builder()
            .userId(staffUserId)
            .recipientRole("STAFF")
            .title(String.format("Cước trả hàng - Đơn %s", orderCode))
            .description(description)
            .notificationType(NotificationTypeEnum.STAFF_RETURN_PAYMENT)
            .relatedOrderId(orderId)
            .relatedIssueId(issueId)
            .metadata(metadata)
            .build();
    }
    
    /**
     * STAFF_ORDER_CANCELLED - Đơn/kiện hàng bị hủy (cho Staff)
     */
    public static CreateNotificationRequest buildStaffOrderCancelled(
        UUID staffUserId,
        String orderCode,
        int cancelledCount,
        int totalPackageCount,
        String cancelReason,
        String customerName,
        UUID orderId
    ) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("orderCode", orderCode);
        metadata.put("cancelledCount", cancelledCount);
        metadata.put("totalPackageCount", totalPackageCount);
        metadata.put("cancelReason", cancelReason);
        metadata.put("customerName", customerName);
        
        String title;
        String description;
        
        if (cancelledCount == totalPackageCount) {
            title = String.format("Đơn %s đã hủy", orderCode);
            description = String.format("Đơn hàng %s của %s đã bị hủy do: %s", orderCode, customerName, cancelReason);
        } else {
            title = String.format("%d kiện đơn %s đã hủy", cancelledCount, orderCode);
            description = String.format("%d/%d kiện của đơn %s (%s) đã bị hủy do: %s", 
                cancelledCount, totalPackageCount, orderCode, customerName, cancelReason);
        }
        
        return CreateNotificationRequest.builder()
            .userId(staffUserId)
            .recipientRole("STAFF")
            .title(title)
            .description(description)
            .notificationType(NotificationTypeEnum.STAFF_ORDER_CANCELLED)
            .relatedOrderId(orderId)
            .metadata(metadata)
            .build();
    }
    
    /**
     * STAFF_PAYMENT_REMINDER - Nhắc nhở liên hệ khách thanh toán (cho Staff)
     */
    public static CreateNotificationRequest buildStaffPaymentReminder(
        UUID staffUserId,
        String orderCode,
        String customerName,
        String customerPhone,
        String paymentType,
        LocalDateTime deadline,
        double amount,
        UUID orderId
    ) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("orderCode", orderCode);
        metadata.put("customerName", customerName);
        metadata.put("customerPhone", customerPhone);
        metadata.put("paymentType", paymentType);
        metadata.put("amount", String.format("%,.0f VNĐ", amount));
        if (deadline != null) {
            metadata.put("deadline", deadline.format(DATE_FORMATTER));
        }
        
        String paymentTypeName = switch (paymentType) {
            case "CONTRACT_SIGN" -> "ký hợp đồng";
            case "DEPOSIT" -> "thanh toán cọc";
            case "FULL_PAYMENT" -> "thanh toán đủ";
            case "RETURN_SHIPPING" -> "thanh toán cước trả hàng";
            default -> "thanh toán";
        };
        
        String description = String.format(
            "Đơn %s sắp hết hạn %s. Vui lòng liên hệ khách hàng %s (%s) để nhắc nhở thanh toán %,.0f VNĐ trước %s.",
            orderCode,
            paymentTypeName,
            customerName,
            customerPhone,
            amount,
            deadline != null ? deadline.format(DATE_FORMATTER) : "hạn thanh toán"
        );
        
        return CreateNotificationRequest.builder()
            .userId(staffUserId)
            .recipientRole("STAFF")
            .title(String.format("Nhắc: %s - Đơn %s", paymentTypeName, orderCode))
            .description(description)
            .notificationType(NotificationTypeEnum.STAFF_PAYMENT_REMINDER)
            .relatedOrderId(orderId)
            .metadata(metadata)
            .build();
    }
    
    // ============= ISSUE NOTIFICATIONS =============
    
    /**
     * NEW_ISSUE_REPORTED - Sự cố mới cần xử lý (cho Staff)
     */
    public static CreateNotificationRequest buildNewIssueReported(
        UUID userId,
        String issueCode,
        String issueType,
        String orderCode,
        String driverName,
        String driverPhone,
        String vehiclePlate,
        UUID orderId,
        UUID issueId,
        UUID vehicleAssignmentId
    ) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("issueCode", issueCode);
        metadata.put("issueType", issueType);
        metadata.put("orderCode", orderCode);
        metadata.put("driverName", driverName);
        metadata.put("driverPhone", driverPhone);
        metadata.put("vehiclePlate", vehiclePlate);
        metadata.put("priority", "HIGH");
        
        String description = String.format(
            "Tài xế %s đã báo cáo sự cố \"%s\" cho đơn hàng %s. Vui lòng xử lý sớm nhất.",
            driverName,
            issueType,
            orderCode
        );
        
        return CreateNotificationRequest.builder()
            .userId(userId)
            .recipientRole("STAFF")
            .title(String.format("Sự cố mới: %s - Đơn %s", issueType, orderCode))
            .description(description)
            .notificationType(NotificationTypeEnum.NEW_ISSUE_REPORTED)
            .relatedOrderId(orderId)
            .relatedIssueId(issueId)
            .relatedVehicleAssignmentId(vehicleAssignmentId)
            .metadata(metadata)
            .build();
    }
    
    /**
     * PACKAGE_DAMAGED - Kiện hàng bị hư hỏng (cho Customer)
     * Email thông báo chi tiết về hư hỏng và hướng dẫn đền bù
     */
    public static CreateNotificationRequest buildPackageDamaged(
        UUID userId,
        String orderCode,
        String issueCode,
        int damagedCount,
        int totalPackageCount,
        List<OrderDetailEntity> damagedPackages,
        UUID orderId,
        UUID issueId,
        List<UUID> damagedOrderDetailIds,
        UUID vehicleAssignmentId
    ) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("orderCode", orderCode);
        metadata.put("issueCode", issueCode);
        metadata.put("damagedCount", damagedCount);
        metadata.put("totalPackageCount", totalPackageCount);
        metadata.put("issueType", "DAMAGE");
        metadata.put("actionRequired", "CONTACT_STAFF");
        
        // Thêm thông tin chi tiết về các kiện hàng bị hư hỏng
        List<Map<String, Object>> packageDetails = createPackageMetadata(damagedPackages);
        metadata.put("damagedPackages", packageDetails);
        
        String description = String.format(
            "⚠️ THÔNG BÁO SỰ CỐ HƯ HỎNG HÀNG HÓA\n\n" +
            "Chúng tôi rất tiếc phải thông báo rằng %d/%d kiện hàng trong đơn hàng %s đã bị hư hỏng trong quá trình vận chuyển.\n\n" +
            "📋 THÔNG TIN SỰ CỐ:\n" +
            "• Mã sự cố: %s\n" +
            "• Số kiện hàng bị ảnh hưởng: %d kiện\n" +
            "• Nguyên nhân: Do phía vận chuyển\n\n" +
            "📦 CHI TIẾT KIỆN HÀNG BỊ HƯ HỎNG:\n" +
            "%s\n\n" +
            "💰 HƯỚNG DẪN YÊU CẦU BỒI THƯỜNG:\n" +
            "1. Vui lòng liên hệ với nhân viên hỗ trợ của chúng tôi qua hotline hoặc email\n" +
            "2. Cung cấp mã sự cố %s để được hỗ trợ nhanh nhất\n" +
            "3. Mức bồi thường sẽ được tính theo điều khoản trong hợp đồng vận chuyển\n\n" +
            "Chúng tôi cam kết xử lý và bồi thường theo đúng quy định trong hợp đồng đã ký kết.",
            damagedCount, totalPackageCount, orderCode,
            issueCode, damagedCount, formatPackageList(damagedPackages), issueCode
        );
        
        return CreateNotificationRequest.builder()
            .userId(userId)
            .recipientRole("CUSTOMER")
            .title(String.format("⚠️ Sự cố hư hỏng hàng hóa - Đơn %s", orderCode))
            .description(description)
            .notificationType(NotificationTypeEnum.PACKAGE_DAMAGED)
            .relatedOrderId(orderId)
            .relatedIssueId(issueId)
            .relatedOrderDetailIds(damagedOrderDetailIds)
            .relatedVehicleAssignmentId(vehicleAssignmentId)
            .metadata(metadata)
            .build();
    }
    
    /**
     * ORDER_REJECTED_BY_RECEIVER - Người nhận từ chối nhận hàng (cho Customer)
     * Email yêu cầu thanh toán cước trả hàng với deadline và cảnh báo
     */
    public static CreateNotificationRequest buildOrderRejectedByReceiver(
        UUID userId,
        String orderCode,
        String issueCode,
        int rejectedCount,
        int totalPackageCount,
        String deliveryLocation,
        List<OrderDetailEntity> rejectedPackages,
        UUID orderId,
        UUID issueId,
        List<UUID> rejectedOrderDetailIds,
        UUID vehicleAssignmentId
    ) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("orderCode", orderCode);
        metadata.put("issueCode", issueCode);
        metadata.put("rejectedCount", rejectedCount);
        metadata.put("totalPackageCount", totalPackageCount);
        metadata.put("deliveryLocation", deliveryLocation);
        metadata.put("issueType", "ORDER_REJECTION");
        metadata.put("actionRequired", "PAYMENT_RETURN_FEE");
        metadata.put("deadlineDays", 3);
        
        // Thêm thông tin chi tiết về các kiện hàng bị từ chối
        List<Map<String, Object>> packageDetails = createPackageMetadata(rejectedPackages);
        metadata.put("rejectedPackages", packageDetails);
        
        String description = String.format(
            "🚫 THÔNG BÁO TỪ CHỐI NHẬN HÀNG\n\n" +
            "Người nhận tại địa chỉ %s đã từ chối nhận %d/%d kiện hàng trong đơn hàng %s.\n\n" +
            "📋 THÔNG TIN SỰ CỐ:\n" +
            "• Mã sự cố: %s\n" +
            "• Số kiện hàng bị từ chối: %d kiện\n" +
            "• Địa điểm giao hàng: %s\n\n" +
            "📦 CHI TIẾT KIỆN HÀNG BỊ TỪ CHỐI:\n" +
            "%s\n\n" +
            "⚡ YÊU CẦU HÀNH ĐỘNG:\n" +
            "Để tiến hành trả hàng về điểm lấy hàng, vui lòng:\n" +
            "1. Truy cập trang Chi tiết đơn hàng → Mục \"Vấn đề trả hàng\"\n" +
            "2. Thanh toán cước phí trả hàng để tài xế tiến hành trả hàng\n\n" +
            "⏰ THỜI HẠN THANH TOÁN: 3 NGÀY\n\n" +
            "⚠️ LƯU Ý QUAN TRỌNG:\n" +
            "Nếu quý khách không thanh toán cước trả hàng trong thời hạn quy định, " +
            "phía vận chuyển sẽ KHÔNG chịu trách nhiệm với các kiện hàng bị từ chối này. " +
            "Hàng hóa có thể bị xử lý theo quy định của công ty.",
            deliveryLocation, rejectedCount, totalPackageCount, orderCode,
            issueCode, rejectedCount, deliveryLocation, formatPackageList(rejectedPackages)
        );
        
        return CreateNotificationRequest.builder()
            .userId(userId)
            .recipientRole("CUSTOMER")
            .title(String.format("🚫 Người nhận từ chối nhận hàng - Đơn %s", orderCode))
            .description(description)
            .notificationType(NotificationTypeEnum.ORDER_REJECTED_BY_RECEIVER)
            .relatedOrderId(orderId)
            .relatedIssueId(issueId)
            .relatedOrderDetailIds(rejectedOrderDetailIds)
            .relatedVehicleAssignmentId(vehicleAssignmentId)
            .metadata(metadata)
            .build();
    }
    
    // ============= SEAL ASSIGNMENT NOTIFICATIONS =============
    
    /**
     * SEAL_ASSIGNED - Seal mới được gán cho chuyến xe
     */
    public static CreateNotificationRequest buildSealAssigned(
        UUID userId,
        String orderCode,
        String sealCode,
        String sealDescription,
        String vehicleTrackingCode,
        List<OrderDetailEntity> packages,
        UUID orderId,
        UUID vehicleAssignmentId
    ) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("orderCode", orderCode);
        metadata.put("sealCode", sealCode);
        metadata.put("sealDescription", sealDescription);
        metadata.put("vehicleTrackingCode", vehicleTrackingCode);
        
        // Thêm thông tin chi tiết về các kiện hàng được bảo vệ bởi seal
        List<Map<String, Object>> packageDetails = createPackageMetadata(packages);
        metadata.put("packages", packageDetails);
        
        String description = String.format(
            "Seal %s đã được gán cho chuyến xe %s. Mã seal này sẽ được sử dụng để đảm bảo an toàn cho hàng hóa của bạn.\n\n" +
            "📦 CHI TIẾT KIỆN HÀNG ĐƯỢC BẢO VỆ:\n" +
            "%s",
            sealCode,
            vehicleTrackingCode,
            formatPackageList(packages)
        );
        
        return CreateNotificationRequest.builder()
            .userId(userId)
            .recipientRole("CUSTOMER")
            .title(String.format("Seal mới được gán - Đơn %s", orderCode))
            .description(description)
            .notificationType(NotificationTypeEnum.SEAL_ASSIGNED)
            .relatedOrderId(orderId)
            .relatedVehicleAssignmentId(vehicleAssignmentId)
            .metadata(metadata)
            .build();
    }
    
    // ============= SEAL REPLACEMENT NOTIFICATIONS =============
    
    /**
     * SEAL_REPLACED - Seal đã được thay thế (Staff gán seal mới)
     */
    public static CreateNotificationRequest buildSealReplaced(
        UUID userId,
        String orderCode,
        String issueCode,
        String oldSealCode,
        String newSealCode,
        String staffName,
        String reason,
        UUID orderId,
        UUID issueId,
        UUID vehicleAssignmentId
    ) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("orderCode", orderCode);
        metadata.put("issueCode", issueCode);
        metadata.put("oldSealCode", oldSealCode);
        metadata.put("newSealCode", newSealCode);
        metadata.put("staffName", staffName);
        metadata.put("reason", reason);
        
        String description = String.format(
            "Seal %s đã được tháo do: %s. Nhân viên %s đã gán seal mới %s. Tài xế sẽ xác nhận việc thay seal sớm nhất.",
            oldSealCode,
            reason,
            staffName,
            newSealCode
        );
        
        return CreateNotificationRequest.builder()
            .userId(userId)
            .recipientRole("CUSTOMER")
            .title(String.format("Seal đã được thay thế - Đơn %s", orderCode))
            .description(description)
            .notificationType(NotificationTypeEnum.SEAL_REPLACED)
            .relatedOrderId(orderId)
            .relatedIssueId(issueId)
            .relatedVehicleAssignmentId(vehicleAssignmentId)
            .metadata(metadata)
            .build();
    }
    
    /**
     * SEAL_REPLACEMENT_COMPLETED - Seal đã được thay thế hoàn tất (Driver xác nhận) - NO EMAIL
     */
    public static CreateNotificationRequest buildSealReplacementCompleted(
        UUID userId,
        String orderCode,
        String issueCode,
        String oldSealCode,
        String newSealCode,
        String driverName,
        String newSealImageUrl,
        String oldSealRemovalImageUrl,
        UUID orderId,
        UUID issueId,
        UUID vehicleAssignmentId
    ) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("orderCode", orderCode);
        metadata.put("issueCode", issueCode);
        metadata.put("oldSealCode", oldSealCode);
        metadata.put("newSealCode", newSealCode);
        metadata.put("driverName", driverName);
        metadata.put("newSealImageUrl", newSealImageUrl);
        metadata.put("oldSealRemovalImageUrl", oldSealRemovalImageUrl);
        
        String description = String.format(
            "Tài xế %s đã xác nhận thay thế seal thành công. Seal cũ %s đã được tháo, seal mới %s đã được gắn vào chuyến hàng của bạn.",
            driverName,
            oldSealCode,
            newSealCode
        );
        
        return CreateNotificationRequest.builder()
            .userId(userId)
            .recipientRole("CUSTOMER")
            .title(String.format("Seal đã được thay thế hoàn tất - Đơn %s", orderCode))
            .description(description)
            .notificationType(NotificationTypeEnum.SEAL_REPLACEMENT_COMPLETED)
            .relatedOrderId(orderId)
            .relatedIssueId(issueId)
            .relatedVehicleAssignmentId(vehicleAssignmentId)
            .metadata(metadata)
            .build();
    }
    
    // ============= ORDER CANCELLATION NOTIFICATIONS =============
    
    /**
     * ORDER_CANCELLED - Đơn hàng bị hủy
     */
    public static CreateNotificationRequest buildOrderCancelled(
        UUID userId,
        String orderCode,
        String cancelReason,
        UUID orderId
    ) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("orderCode", orderCode);
        metadata.put("cancelReason", cancelReason);
        
        String description = String.format(
            "Đơn hàng của bạn đã bị hủy do: %s",
            cancelReason
        );
        
        return CreateNotificationRequest.builder()
            .userId(userId)
            .recipientRole("CUSTOMER")
            .title(String.format("Đơn hàng %s đã bị hủy", orderCode))
            .description(description)
            .notificationType(NotificationTypeEnum.ORDER_CANCELLED)
            .relatedOrderId(orderId)
            .metadata(metadata)
            .build();
    }
    
    // ============= UTILITY METHODS =============
    
    /**
     * Generate issue code từ issue ID
     */
    public static String generateIssueCode(UUID issueId) {
        return "ISS-" + issueId.toString().substring(0, 8).toUpperCase();
    }
    
    /**
     * Format currency amount
     */
    public static String formatCurrency(double amount) {
        return String.format("%,.0f VNĐ", amount);
    }
    
    /**
     * Format datetime
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATE_FORMATTER) : "";
    }
}
