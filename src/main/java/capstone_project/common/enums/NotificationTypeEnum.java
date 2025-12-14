package capstone_project.common.enums;

public enum NotificationTypeEnum {
    // ============= CUSTOMER NOTIFICATIONS - ORDER LIFECYCLE =============
    ORDER_CREATED,              // Đơn hàng đã tạo thành công (Email: YES)
    ORDER_PROCESSING,           // Đơn hàng đang được xử lý (Email: NO)
    CONTRACT_READY,             // Hợp đồng đã sẵn sàng để ký (Email: YES - ACTION)
    CONTRACT_SIGNED,            // Hợp đồng đã được ký (Email: NO)
    PAYMENT_DEPOSIT_SUCCESS,    // Thanh toán cọc thành công (Email: NO)
    PAYMENT_FULL_SUCCESS,       // Thanh toán đủ thành công (Email: NO)
    DRIVER_ASSIGNED,            // Đã phân công tài xế - cần thanh toán (Email: YES - ACTION)
    
    // ============= CUSTOMER NOTIFICATIONS - DELIVERY TRACKING =============
    PICKING_UP_STARTED,         // Tài xế bắt đầu lấy hàng (Email: YES - để vào xem live tracking)
    DELIVERY_STARTED,           // Đang vận chuyển hàng (Email: NO)
    DELIVERY_IN_PROGRESS,       // Sắp giao hàng - gần điểm giao (Email: NO)
    DELIVERY_COMPLETED,         // Giao hàng thành công (Email: YES khi ALL packages)
    DELIVERY_FAILED,            // Giao hàng thất bại (Email: YES)
    
    // ============= CUSTOMER NOTIFICATIONS - CANCELLATION & RETURN =============
    ORDER_CANCELLED,            // Đơn hàng/kiện hàng bị hủy (Email: YES)
    RETURN_STARTED,             // Bắt đầu trả hàng - cần TT cước trả (Email: YES - ACTION)
    RETURN_COMPLETED,           // Trả hàng thành công (Email: YES khi ALL packages)
    RETURN_PAYMENT_REQUIRED,    // Cần thanh toán cước trả hàng (Email: YES - ACTION)
    COMPENSATION_PROCESSED,     // Bồi thường đã xử lý (Email: YES)

    // ============= CUSTOMER NOTIFICATIONS - ISSUES =============
    ISSUE_REPORTED,             // Sự cố đã được báo cáo (Email: NO)
    ISSUE_IN_PROGRESS,          // Sự cố đang xử lý (Email: NO)
    ISSUE_RESOLVED,             // Sự cố đã giải quyết (Email: NO)
    PACKAGE_DAMAGED,            // Hàng bị hư hỏng (Email: YES)
    ORDER_REJECTED_BY_RECEIVER, // Người nhận từ chối nhận hàng (Email: YES)
    REROUTE_REQUIRED,           // Cần tái định tuyến (Email: NO)
    SEAL_REPLACED,              // Seal đã được thay thế (Email: YES)
    SEAL_REPLACEMENT_COMPLETED, // Seal đã được thay thế hoàn tất (Email: NO)
    SEAL_ASSIGNED,              // Được cấp seal mới (Email: YES)

    // ============= CUSTOMER NOTIFICATIONS - REMINDERS =============
    PAYMENT_REMINDER,           // Nhắc nhở thanh toán (Email: YES - ACTION)
    PAYMENT_OVERDUE,            // Quá hạn thanh toán (Email: YES)
    CONTRACT_SIGN_REMINDER,     // Nhắc nhở ký hợp đồng (Email: YES - ACTION)
    CONTRACT_SIGN_OVERDUE,      // Quá hạn ký hợp đồng (Email: YES)

    // ============= STAFF NOTIFICATIONS - ORDER MANAGEMENT =============
    STAFF_ORDER_CREATED,        // Đơn hàng mới được tạo
    STAFF_ORDER_PROCESSING,     // Đơn hàng cần tạo hợp đồng
    STAFF_CONTRACT_SIGNED,      // Hợp đồng đã được ký
    STAFF_DEPOSIT_RECEIVED,     // Đã nhận cọc - cần lên lộ trình
    STAFF_FULL_PAYMENT,         // Đã thanh toán đủ
    STAFF_RETURN_PAYMENT,       // Cước trả hàng đã thanh toán
    STAFF_ORDER_CANCELLED,      // Đơn/kiện hàng bị hủy
    STAFF_PAYMENT_REMINDER,     // Nhắc nhở liên hệ khách thanh toán
    STAFF_DELIVERY_COMPLETED,   // Giao hàng hoàn tất (Staff, no email)
    STAFF_RETURN_COMPLETED,     // Trả hàng hoàn tất (Staff, no email)
    STAFF_RETURN_IN_PROGRESS,   // Đang trả hàng (Staff, no email)
    CUSTOMER_RETURN_IN_PROGRESS,   // Bắt đầu trả hàng (customer đã thanh toán)

    // ============= STAFF NOTIFICATIONS - ISSUES =============
    NEW_ISSUE_REPORTED,         // Có sự cố mới cần xử lý
    ISSUE_ESCALATED,            // Sự cố cần xử lý gấp

    // ============= STAFF NOTIFICATIONS - MAINTENANCE (future) =============
    VEHICLE_MAINTENANCE_DUE,    // Đến hạn bảo trì
    VEHICLE_INSPECTION_DUE,     // Đến hạn kiểm định

    // ============= DRIVER NOTIFICATIONS =============
    NEW_ORDER_ASSIGNED,         // Đơn hàng mới được phân công
    PAYMENT_RECEIVED,           // Khách đã thanh toán đủ
    RETURN_PAYMENT_SUCCESS,     // Thanh toán cước trả hàng thành công
    DAMAGE_RESOLVED,            // Sự cố hư hỏng đã giải quyết
    ORDER_REJECTION_RESOLVED,   // Sự cố khách từ chối đã giải quyết
    DRIVER_CREATED,             // Tài khoản tài xế được tạo - gửi thông tin đăng nhập (Email: YES)
    
    // ============= STAFF NOTIFICATIONS - ACCOUNT =============
    STAFF_CREATED;              // Tài khoản nhân viên được tạo - gửi thông tin đăng nhập (Email: YES)

    // Vietnamese translation method
    public String getVietnameseLabel() {
        switch (this) {
            // Customer notifications - Order lifecycle
            case ORDER_CREATED: return "Đơn hàng mới";
            case ORDER_PROCESSING: return "Đang xử lý";
            case CONTRACT_READY: return "Hợp đồng sẵn sàng";
            case CONTRACT_SIGNED: return "Đã ký hợp đồng";
            case PAYMENT_DEPOSIT_SUCCESS: return "Đã thanh toán cọc";
            case PAYMENT_FULL_SUCCESS: return "Đã thanh toán đủ";
            case DRIVER_ASSIGNED: return "Đã phân tài xế";
            
            // Customer notifications - Delivery tracking
            case PICKING_UP_STARTED: return "Đang lấy hàng";
            case DELIVERY_STARTED: return "Đang vận chuyển";
            case DELIVERY_IN_PROGRESS: return "Đang giao hàng";
            case DELIVERY_COMPLETED: return "Đã giao hàng";
            case DELIVERY_FAILED: return "Giao hàng thất bại";
            
            // Customer notifications - Cancellation & return
            case ORDER_CANCELLED: return "Đã hủy";
            case RETURN_STARTED: return "Bắt đầu trả hàng";
            case RETURN_COMPLETED: return "Trả hàng thành công";
            case RETURN_PAYMENT_REQUIRED: return "Cần thanh toán cước trả";
            case COMPENSATION_PROCESSED: return "Đã xử lý bồi thường";
            case CUSTOMER_RETURN_IN_PROGRESS: return "Đang trả hàng";
            
            // Customer notifications - Issues
            case ISSUE_REPORTED: return "Đã báo cáo sự cố";
            case ISSUE_IN_PROGRESS: return "Đang xử lý sự cố";
            case ISSUE_RESOLVED: return "Đã giải quyết sự cố";
            case PACKAGE_DAMAGED: return "Hàng hư hỏng";
            case ORDER_REJECTED_BY_RECEIVER: return "Bị từ chối";
            case REROUTE_REQUIRED: return "Cần tái định tuyến";
            case SEAL_REPLACED: return "Đã thay thế seal";
            case SEAL_REPLACEMENT_COMPLETED: return "Thay thế seal hoàn tất";
            case SEAL_ASSIGNED: return "Đã cấp seal";
            
            // Customer notifications - Reminders
            case PAYMENT_REMINDER: return "Nhắc nhở thanh toán";
            case PAYMENT_OVERDUE: return "Quá hạn thanh toán";
            case CONTRACT_SIGN_REMINDER: return "Nhắc nhở ký hợp đồng";
            case CONTRACT_SIGN_OVERDUE: return "Quá hạn ký hợp đồng";
            
            // Staff notifications
            case STAFF_ORDER_CREATED: return "Đơn hàng mới";
            case STAFF_ORDER_PROCESSING: return "Đơn hàng cần xử lý";
            case STAFF_CONTRACT_SIGNED: return "Đã ký hợp đồng";
            case STAFF_DEPOSIT_RECEIVED: return "Đã nhận cọc";
            case STAFF_FULL_PAYMENT: return "Đã thanh toán đủ";
            case STAFF_RETURN_PAYMENT: return "Đã thanh toán cước trả";
            case STAFF_ORDER_CANCELLED: return "Đơn hàng bị hủy";
            case STAFF_PAYMENT_REMINDER: return "Nhắc nhở thanh toán";
            case STAFF_DELIVERY_COMPLETED: return "Đã giao hàng";
            case STAFF_RETURN_COMPLETED: return "Đã trả hàng";
            case STAFF_RETURN_IN_PROGRESS: return "Đang trả hàng";
            case NEW_ISSUE_REPORTED: return "Sự cố mới";
            case ISSUE_ESCALATED: return "Sự cố khẩn cấp";
            case VEHICLE_MAINTENANCE_DUE: return "Đến hạn bảo trì";
            case VEHICLE_INSPECTION_DUE: return "Đến hạn kiểm định";
            
            // Driver notifications
            case NEW_ORDER_ASSIGNED: return "Đơn hàng mới";
            case PAYMENT_RECEIVED: return "Đã nhận thanh toán";
            case RETURN_PAYMENT_SUCCESS: return "Thanh toán cước trả thành công";
            case DAMAGE_RESOLVED: return "Đã giải quyết hư hỏng";
            case ORDER_REJECTION_RESOLVED: return "Đã giải quyết từ chối";
            case DRIVER_CREATED: return "Tài khoản được tạo";
            case STAFF_CREATED: return "Tài khoản được tạo";
            
            default: return "Thông báo";
        }
    }

    // Helper method to get Vietnamese label with null safety
    public static String getVietnameseLabel(NotificationTypeEnum type) {
        return type != null ? type.getVietnameseLabel() : "Thông báo";
    }
}
