package capstone_project.common.enums;

public enum OrderDetailStatusEnum {
    PENDING,
    ON_PLANNING,
    ASSIGNED_TO_DRIVER,
    PICKING_UP,
    ON_DELIVERED,
    ONGOING_DELIVERED,
    DELIVERED,      // Trạng thái cuối cùng khi giao hàng thành công
    IN_TROUBLES,
    COMPENSATION,   // Trạng thái cuối cùng khi có bồi thường
    RETURNING,      // Customer đã thanh toán cước trả hàng, driver đang trả hàng về pickup
    RETURNED,       // Trạng thái cuối cùng khi trả hàng thành công
    CANCELLED       // Customer không thanh toán cước trả hàng, hủy kiện hàng
}
