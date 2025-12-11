package capstone_project.common.enums;

/**
 * Enum định nghĩa trạng thái của xe
 */
public enum VehicleStatusEnum {
    /**
     * Xe hoạt động bình thường, sẵn sàng phân công
     */
    ACTIVE,

    /**
     * Xe không hoạt động (tạm ngưng)
     */
    INACTIVE,

    /**
     * Xe đang bảo trì
     */
    MAINTENANCE,

    /**
     * Xe đang vận chuyển
     */
    IN_TRANSIT,

    /**
     * Xe hỏng
     */
    BREAKDOWN,

    /**
     * Xe gặp tai nạn
     */
    ACCIDENT,

    /**
     * Xe hết hạn đăng kiểm - không được phân công
     */
    INSPECTION_EXPIRED,

    /**
     * Xe hết hạn bảo hiểm - không được phân công
     */
    INSURANCE_EXPIRED,

    /**
     * Xe sắp đến hạn đăng kiểm (trong vòng 30 ngày) - cảnh báo, vẫn được phân công
     */
    INSPECTION_DUE,

    /**
     * Xe sắp đến hạn bảo hiểm (trong vòng 30 ngày) - cảnh báo, vẫn được phân công
     */
    INSURANCE_DUE,

    /**
     * Xe sắp đến hạn bảo dưỡng (trong vòng 30 ngày) - cảnh báo, vẫn được phân công
     */
    MAINTENANCE_DUE,

    /**
     * @deprecated Sử dụng INSPECTION_DUE thay thế
     */
    @Deprecated
    INSPECTION_EXPIRING_SOON,

    /**
     * @deprecated Sử dụng INSURANCE_DUE thay thế
     */
    @Deprecated
    INSURANCE_EXPIRING_SOON
}
