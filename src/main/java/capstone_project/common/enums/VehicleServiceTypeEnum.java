package capstone_project.common.enums;

/**
 * Enum định nghĩa các loại dịch vụ/bảo trì xe
 * Dùng chung cho cả đăng kiểm và bảo trì
 */
public enum VehicleServiceTypeEnum {
    /**
     * Đăng kiểm định kỳ
     */
    INSPECTION,
    
    /**
     * Bảo trì định kỳ (thay dầu, lọc gió, etc.)
     */
    MAINTENANCE_PERIODIC,
    
    /**
     * Sửa chữa đột xuất
     */
    MAINTENANCE_REPAIR,
    
    /**
     * Bảo hiểm (gia hạn/cập nhật)
     */
    INSURANCE_RENEWAL,
    
    /**
     * Khác
     */
    OTHER
}
