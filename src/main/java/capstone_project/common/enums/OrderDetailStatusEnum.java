package capstone_project.common.enums;

public enum OrderDetailStatusEnum {
    PENDING,
    ON_PLANNING,
    ASSIGNED_TO_DRIVER,
    PICKING_UP,
    ON_DELIVERED,
    ONGOING_DELIVERED,
    DELIVERED,
    SUCCESSFUL,
    IN_TROUBLES,
    COMPENSATION,
    RETURNING,      // Customer đã thanh toán cước trả hàng, driver đang trả hàng về pickup
    RETURNED,       // Driver đã trả hàng về pickup thành công
    CANCELLED       // Customer không thanh toán cước trả hàng, hủy kiện hàng
}
