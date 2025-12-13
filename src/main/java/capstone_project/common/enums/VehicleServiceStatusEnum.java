package capstone_project.common.enums;

/**
 * Enum định nghĩa trạng thái của dịch vụ/bảo trì xe
 */
public enum VehicleServiceStatusEnum {
    PLANNED,
    /**
     * Đã hoàn thành
     */
    COMPLETED,
    
    /**
     * Đã hủy
     */
    CANCELLED,
    
    /**
     * Quá hạn (đã qua ngày dự kiến nhưng chưa hoàn thành)
     */
    OVERDUE
}
