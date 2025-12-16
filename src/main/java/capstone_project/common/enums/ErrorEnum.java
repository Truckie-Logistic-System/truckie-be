package capstone_project.common.enums;

import lombok.Getter;

@Getter
public enum ErrorEnum {
    USER_BY_ID_NOT_FOUND(1, "Không tìm thấy người dùng với ID này"),
    USER_NAME_OR_EMAIL_EXISTED(2, "Tên đăng nhập hoặc email đã tồn tại"),
    USER_PERMISSION_DENIED(10, "Bạn không có quyền truy cập tài nguyên này"),
    INTERNAL_SERVER_ERROR(3, "Đã xảy ra lỗi hệ thống. Vui lòng thử lại sau!"),
    LOGIN_NOT_FOUND_USER_NAME_OR_EMAIL(4, "Không tìm thấy tên đăng nhập hoặc email"),
    LOGIN_WRONG_PASSWORD(5, "Mật khẩu không đúng"),
    INVALID_DATE_FORMAT(6, "Định dạng ngày không hợp lệ. Định dạng mong đợi: yyyy-MM-dd"),
    ALREADY_EXISTED(9, "Đã tồn tại"),
    NULL(13, "Trường này không được để trống"),
    REQUIRED(14, "Trường này là bắt buộc"),
    NOT_FOUND(15, "Không tìm thấy"),
    UNAUTHORIZED(401, "Chưa xác thực - Yêu cầu đăng nhập"),
    // VEHICLE BASED ERROR
    VEHICLE_TYPE_NOT_FOUND(15, "Không tìm thấy loại phương tiện"),
    NO_VEHICLE_AVAILABLE(24, "Không có phương tiện khả dụng"),
    VEHICLE_NOT_FOUND(25, "Không tìm thấy phương tiện"),
    VEHICLE_NOT_AVAILABLE(30, "Phương tiện hiện đang được sử dụng cho chuyến khác"),
    DRIVER_NOT_AVAILABLE(31, "Tài xế hiện đang được phân công cho chuyến khác"),
    //
    INVALID(16, "Yêu cầu không hợp lệ"),
    ENUM_INVALID(17, "Giá trị enum không hợp lệ"),
    INVALID_EMAIL(18, "Định dạng email không hợp lệ"),
    INVALID_REQUEST(19, "Yêu cầu không hợp lệ"),
    ROLE_NOT_FOUND(20, "Không tìm thấy vai trò"),
    NEW_PASSWORD_MUST_BE_DIFFERENT_OLD_PASSWORD(21, "Mật khẩu mới phải khác mật khẩu cũ"),
    OLD_PASSWORD_IS_INCORRECT(22, "Mật khẩu cũ không đúng"),
    PASSWORD_CONFIRM_NOT_MATCH(23, "Mật khẩu xác nhận không khớp"),
    ENTITY_NOT_FOUND(26, "Không tìm thấy thực thể"),
    // ISSUE & REFUND BASED ERROR
    ISSUE_NOT_FOUND(32, "Không tìm thấy sự cố"),
    ORDER_DETAIL_NOT_FOUND(33, "Không tìm thấy chi tiết đơn hàng"),
    REFUND_NOT_FOUND(34, "Không tìm thấy yêu cầu hoàn tiền"),
    ;
    private final String message;
    private final long errorCode;

    ErrorEnum(final long errorCode, final String message) {
        this.errorCode = errorCode;
        this.message = message;
    }
}
