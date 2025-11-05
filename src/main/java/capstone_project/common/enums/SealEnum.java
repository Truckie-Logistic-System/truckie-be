package capstone_project.common.enums;

/**
 * Enum for seal status lifecycle
 */
public enum SealEnum {
    ACTIVE,      // Seal sẵn sàng để sử dụng (chưa gắn)
    IN_USE,      // Seal đang được gắn trên container
    REMOVED,     // Seal đã bị gỡ (có issue liên quan)
    REPLACED,    // Seal đã được thay thế bởi seal khác
    DAMAGED,     // Seal bị hỏng
    EXPIRED      // Seal hết hạn
}
