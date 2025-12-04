package capstone_project.common.enums;

/**
 * Enum for DAMAGE compensation processing status
 */
public enum DamageCompensationStatusEnum {
    /**
     * Chờ thẩm định - Staff chưa nhập thông tin đánh giá
     */
    PENDING_ASSESSMENT("Chờ thẩm định"),
    
    /**
     * Đã đề xuất - Staff đã tính toán và đề xuất mức bồi thường
     */
    PROPOSED("Đã đề xuất bồi thường"),
    
    /**
     * Đã phê duyệt - Mức bồi thường đã được phê duyệt
     */
    APPROVED("Đã phê duyệt"),
    
    /**
     * Từ chối - Không bồi thường (lỗi khách hàng, bất khả kháng, v.v.)
     */
    REJECTED("Từ chối bồi thường");
    
    private final String label;
    
    DamageCompensationStatusEnum(String label) {
        this.label = label;
    }
    
    public String getLabel() {
        return label;
    }
}
