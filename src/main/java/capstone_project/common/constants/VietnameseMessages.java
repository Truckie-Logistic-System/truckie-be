package capstone_project.common.constants;

/**
 * Chứa tất cả các message tiếng Việt cho hệ thống.
 * Sử dụng class này để đảm bảo tính nhất quán của các thông báo trong toàn bộ ứng dụng.
 */
public final class VietnameseMessages {

    private VietnameseMessages() {
        // Prevent instantiation
    }

    // ==================== COMMON MESSAGES ====================
    public static final String SUCCESS = "Thành công";
    public static final String FAILED = "Thất bại";
    public static final String NOT_FOUND = "Không tìm thấy";
    public static final String ALREADY_EXISTS = "Đã tồn tại";
    public static final String REQUIRED = "Trường này là bắt buộc";
    public static final String INVALID = "Không hợp lệ";
    public static final String UNAUTHORIZED = "Không có quyền truy cập";
    public static final String INTERNAL_ERROR = "Đã xảy ra lỗi hệ thống. Vui lòng thử lại sau!";
    public static final String NULL_VALUE = "Giá trị không được để trống";

    // ==================== USER MESSAGES ====================
    public static final String USER_NOT_FOUND = "Không tìm thấy người dùng";
    public static final String USER_NOT_FOUND_BY_ID = "Không tìm thấy người dùng với ID: %s";
    public static final String USER_NOT_FOUND_BY_EMAIL = "Không tìm thấy người dùng với email: %s";
    public static final String USER_NOT_FOUND_BY_PHONE = "Không tìm thấy người dùng với số điện thoại: %s";
    public static final String USERNAME_OR_EMAIL_EXISTS = "Tên đăng nhập hoặc email đã tồn tại";
    public static final String EMAIL_EXISTS = "Email đã được sử dụng";
    public static final String PHONE_EXISTS = "Số điện thoại đã được sử dụng";
    public static final String USERNAME_EXISTS = "Tên đăng nhập đã tồn tại";
    public static final String PERMISSION_DENIED = "Bạn không có quyền truy cập tài nguyên này";
    public static final String USER_INACTIVE = "Tài khoản đã bị vô hiệu hóa";
    public static final String USER_SUSPENDED = "Tài khoản đã bị tạm khóa";

    // ==================== AUTH MESSAGES ====================
    public static final String LOGIN_FAILED = "Đăng nhập thất bại";
    public static final String LOGIN_USER_NOT_FOUND = "Không tìm thấy tên đăng nhập hoặc email";
    public static final String LOGIN_WRONG_PASSWORD = "Mật khẩu không đúng";
    public static final String PASSWORD_MISMATCH = "Mật khẩu xác nhận không khớp";
    public static final String OLD_PASSWORD_INCORRECT = "Mật khẩu cũ không đúng";
    public static final String NEW_PASSWORD_SAME_AS_OLD = "Mật khẩu mới phải khác mật khẩu cũ";
    public static final String TOKEN_EXPIRED = "Phiên đăng nhập đã hết hạn";
    public static final String TOKEN_INVALID = "Token không hợp lệ";
    public static final String OTP_INVALID = "Mã OTP không hợp lệ";
    public static final String OTP_EXPIRED = "Mã OTP đã hết hạn";

    // ==================== ROLE MESSAGES ====================
    public static final String ROLE_NOT_FOUND = "Không tìm thấy vai trò";
    public static final String ROLE_NOT_FOUND_BY_ID = "Không tìm thấy vai trò với ID: %s";

    // ==================== VEHICLE MESSAGES ====================
    public static final String VEHICLE_NOT_FOUND = "Không tìm thấy phương tiện";
    public static final String VEHICLE_NOT_FOUND_BY_ID = "Không tìm thấy phương tiện với ID: %s";
    public static final String VEHICLE_TYPE_NOT_FOUND = "Không tìm thấy loại phương tiện";
    public static final String VEHICLE_TYPE_NOT_FOUND_BY_ID = "Không tìm thấy loại phương tiện với ID: %s";
    public static final String VEHICLE_TYPE_NAME_EXISTS = "Tên loại phương tiện đã tồn tại";
    public static final String VEHICLE_NOT_AVAILABLE = "Phương tiện hiện đang được sử dụng cho chuyến khác";
    public static final String NO_VEHICLE_AVAILABLE = "Không có phương tiện khả dụng";
    public static final String LICENSE_PLATE_EXISTS = "Biển số xe đã tồn tại";
    public static final String VEHICLE_IN_MAINTENANCE = "Phương tiện đang trong quá trình bảo trì";
    public static final String VEHICLE_INSPECTION_EXPIRED = "Phương tiện đã hết hạn đăng kiểm";
    public static final String VEHICLE_INSURANCE_EXPIRED = "Phương tiện đã hết hạn bảo hiểm";

    // ==================== DRIVER MESSAGES ====================
    public static final String DRIVER_NOT_FOUND = "Không tìm thấy tài xế";
    public static final String DRIVER_NOT_FOUND_BY_ID = "Không tìm thấy tài xế với ID: %s";
    public static final String DRIVER_NOT_AVAILABLE = "Tài xế hiện đang được phân công cho chuyến khác";
    public static final String DRIVER_INACTIVE = "Tài xế đã bị vô hiệu hóa";
    public static final String DRIVER_SUSPENDED = "Tài xế đã bị tạm khóa";
    public static final String DRIVER_LICENSE_EXPIRED = "Bằng lái xe đã hết hạn";
    public static final String DRIVER_ALREADY_ASSIGNED = "Tài xế đã được phân công cho ngày này";

    // ==================== ORDER MESSAGES ====================
    public static final String ORDER_NOT_FOUND = "Không tìm thấy đơn hàng";
    public static final String ORDER_NOT_FOUND_BY_ID = "Không tìm thấy đơn hàng với ID: %s";
    public static final String ORDER_NOT_FOUND_BY_CODE = "Không tìm thấy đơn hàng với mã: %s";
    public static final String ORDER_DETAIL_NOT_FOUND = "Không tìm thấy chi tiết đơn hàng";
    public static final String ORDER_DETAIL_NOT_FOUND_BY_ID = "Không tìm thấy chi tiết đơn hàng với ID: %s";
    public static final String ORDER_NOT_ON_PLANNING = "Đơn hàng chưa ở trạng thái lên kế hoạch";
    public static final String ORDER_ALREADY_ASSIGNED = "Đơn hàng đã được phân công";
    public static final String ORDER_CANCELLED = "Đơn hàng đã bị hủy";
    public static final String ORDER_COMPLETED = "Đơn hàng đã hoàn thành";
    public static final String ORDER_MISSING_ADDRESS = "Đơn hàng thiếu địa chỉ lấy hàng hoặc giao hàng";
    public static final String ORDER_INVALID_COORDINATES = "Đơn hàng có tọa độ không hợp lệ";

    // ==================== CONTRACT MESSAGES ====================
    public static final String CONTRACT_NOT_FOUND = "Không tìm thấy hợp đồng";
    public static final String CONTRACT_NOT_FOUND_BY_ID = "Không tìm thấy hợp đồng với ID: %s";
    public static final String CONTRACT_ALREADY_SIGNED = "Hợp đồng đã được ký";
    public static final String CONTRACT_EXPIRED = "Hợp đồng đã hết hạn";
    public static final String CONTRACT_RULE_NOT_FOUND = "Không tìm thấy điều khoản hợp đồng";

    // ==================== VEHICLE ASSIGNMENT MESSAGES ====================
    public static final String VEHICLE_ASSIGNMENT_NOT_FOUND = "Không tìm thấy phân công xe";
    public static final String VEHICLE_ASSIGNMENT_NOT_FOUND_BY_ID = "Không tìm thấy phân công xe với ID: %s";
    public static final String VEHICLE_ASSIGNMENT_ALREADY_EXISTS = "Phân công xe đã tồn tại";

    // ==================== FUEL CONSUMPTION MESSAGES ====================
    public static final String FUEL_CONSUMPTION_NOT_FOUND = "Không tìm thấy thông tin tiêu thụ nhiên liệu";
    public static final String FUEL_CONSUMPTION_NOT_FOUND_BY_ID = "Không tìm thấy thông tin tiêu thụ nhiên liệu với ID: %s";
    public static final String FUEL_CONSUMPTION_ALREADY_EXISTS = "Thông tin tiêu thụ nhiên liệu đã tồn tại cho phân công này";
    public static final String ODOMETER_READING_REQUIRED = "Số đồng hồ công tơ mét là bắt buộc";
    public static final String ODOMETER_START_NULL = "Số đồng hồ công tơ mét lúc bắt đầu không được để trống";
    public static final String ODOMETER_END_NULL = "Số đồng hồ công tơ mét lúc kết thúc không được để trống";

    // ==================== ISSUE MESSAGES ====================
    public static final String ISSUE_NOT_FOUND = "Không tìm thấy sự cố";
    public static final String ISSUE_NOT_FOUND_BY_ID = "Không tìm thấy sự cố với ID: %s";
    public static final String ISSUE_TYPE_NOT_FOUND = "Không tìm thấy loại sự cố";
    public static final String ISSUE_ALREADY_RESOLVED = "Sự cố đã được giải quyết";
    public static final String ISSUE_MUST_BE_OPEN = "Sự cố phải ở trạng thái mở để xử lý";

    // ==================== REFUND MESSAGES ====================
    public static final String REFUND_NOT_FOUND = "Không tìm thấy yêu cầu hoàn tiền";
    public static final String REFUND_NOT_FOUND_BY_ID = "Không tìm thấy yêu cầu hoàn tiền với ID: %s";
    public static final String REFUND_ALREADY_PROCESSED = "Yêu cầu hoàn tiền đã được xử lý";

    // ==================== PAYMENT MESSAGES ====================
    public static final String PAYMENT_NOT_FOUND = "Không tìm thấy thông tin thanh toán";
    public static final String PAYMENT_FAILED = "Thanh toán thất bại";
    public static final String PAYMENT_ALREADY_COMPLETED = "Thanh toán đã hoàn thành";
    public static final String PAYMENT_EXPIRED = "Thanh toán đã hết hạn";
    public static final String INSUFFICIENT_BALANCE = "Số dư không đủ";

    // ==================== ADDRESS MESSAGES ====================
    public static final String ADDRESS_NOT_FOUND = "Không tìm thấy địa chỉ";
    public static final String ADDRESS_NOT_FOUND_BY_ID = "Không tìm thấy địa chỉ với ID: %s";
    public static final String PICKUP_ADDRESS_REQUIRED = "Địa chỉ lấy hàng là bắt buộc";
    public static final String DELIVERY_ADDRESS_REQUIRED = "Địa chỉ giao hàng là bắt buộc";

    // ==================== PRICING MESSAGES ====================
    public static final String SIZE_RULE_NOT_FOUND = "Không tìm thấy quy tắc kích thước";
    public static final String SIZE_RULE_NAME_EXISTS = "Tên quy tắc kích thước đã tồn tại";
    public static final String SIZE_RULE_NAME_MISMATCH = "Tên quy tắc phải khớp với tên loại phương tiện";
    public static final String DISTANCE_RULE_NOT_FOUND = "Không tìm thấy quy tắc khoảng cách";
    public static final String BASING_PRICE_NOT_FOUND = "Không tìm thấy giá cơ bản";
    public static final String CATEGORY_NOT_FOUND = "Không tìm thấy danh mục";
    public static final String CATEGORY_ID_REQUIRED = "ID danh mục là bắt buộc";
    public static final String VEHICLE_TYPE_ID_REQUIRED = "ID loại phương tiện là bắt buộc";

    // ==================== DEVICE MESSAGES ====================
    public static final String DEVICE_NOT_FOUND = "Không tìm thấy thiết bị";
    public static final String DEVICE_NOT_FOUND_BY_ID = "Không tìm thấy thiết bị với ID: %s";
    public static final String DEVICE_TYPE_NOT_FOUND = "Không tìm thấy loại thiết bị";
    public static final String DEVICE_ALREADY_ASSIGNED = "Thiết bị đã được gán cho phương tiện khác";

    // ==================== SEAL MESSAGES ====================
    public static final String SEAL_NOT_FOUND = "Không tìm thấy seal";
    public static final String SEAL_NOT_FOUND_BY_ID = "Không tìm thấy seal với ID: %s";
    public static final String SEAL_ALREADY_USED = "Seal đã được sử dụng";
    public static final String SEAL_INVALID = "Seal không hợp lệ";

    // ==================== ROUTE MESSAGES ====================
    public static final String ROUTE_NOT_FOUND = "Không tìm thấy tuyến đường";
    public static final String CARRIER_SETTINGS_NOT_FOUND = "Không tìm thấy cài đặt nhà vận chuyển";
    public static final String ROUTE_CALCULATION_FAILED = "Không thể tính toán tuyến đường";

    // ==================== NOTIFICATION MESSAGES ====================
    public static final String NOTIFICATION_NOT_FOUND = "Không tìm thấy thông báo";
    public static final String NOTIFICATION_SEND_FAILED = "Gửi thông báo thất bại";

    // ==================== CHAT MESSAGES ====================
    public static final String CONVERSATION_NOT_FOUND = "Không tìm thấy cuộc hội thoại";
    public static final String CONVERSATION_NOT_FOUND_BY_ID = "Không tìm thấy cuộc hội thoại với ID: %s";
    public static final String MESSAGE_NOT_FOUND = "Không tìm thấy tin nhắn";

    // ==================== VALIDATION MESSAGES ====================
    public static final String INVALID_DATE_FORMAT = "Định dạng ngày không hợp lệ. Định dạng mong đợi: yyyy-MM-dd";
    public static final String INVALID_EMAIL_FORMAT = "Định dạng email không hợp lệ";
    public static final String INVALID_PHONE_FORMAT = "Định dạng số điện thoại không hợp lệ";
    public static final String INVALID_ENUM_VALUE = "Giá trị enum không hợp lệ";
    public static final String INVALID_REQUEST = "Yêu cầu không hợp lệ";
    public static final String LATITUDE_LONGITUDE_REQUIRED = "Vĩ độ và kinh độ không được để trống";
    public static final String REQUEST_CANNOT_BE_NULL = "Yêu cầu không được để trống";

    // ==================== BILL OF LADING MESSAGES ====================
    public static final String BILL_OF_LADING_NOT_FOUND = "Không tìm thấy vận đơn";
    public static final String BILL_OF_LADING_ALREADY_EXISTS = "Vận đơn đã tồn tại";

    // ==================== COMPENSATION MESSAGES ====================
    public static final String COMPENSATION_NOT_FOUND = "Không tìm thấy thông tin bồi thường";
    public static final String COMPENSATION_ALREADY_PROCESSED = "Bồi thường đã được xử lý";

    // ==================== CUSTOMER MESSAGES ====================
    public static final String CUSTOMER_NOT_FOUND = "Không tìm thấy khách hàng";
    public static final String CUSTOMER_NOT_FOUND_BY_ID = "Không tìm thấy khách hàng với ID: %s";

    // ==================== STAFF MESSAGES ====================
    public static final String STAFF_NOT_FOUND = "Không tìm thấy nhân viên";
    public static final String STAFF_NOT_FOUND_BY_ID = "Không tìm thấy nhân viên với ID: %s";

    // ==================== SERVICE RECORD MESSAGES ====================
    public static final String SERVICE_RECORD_NOT_FOUND = "Không tìm thấy lịch bảo trì/đăng kiểm";
    public static final String SERVICE_RECORD_NOT_FOUND_BY_ID = "Không tìm thấy lịch bảo trì/đăng kiểm với ID: %s";
    public static final String SERVICE_RECORD_ALREADY_STARTED = "Lịch bảo trì/đăng kiểm đã bắt đầu";
    public static final String SERVICE_RECORD_ALREADY_COMPLETED = "Lịch bảo trì/đăng kiểm đã hoàn thành";

    // ==================== ENTITY MESSAGES ====================
    public static final String ENTITY_NOT_FOUND = "Không tìm thấy thực thể";
    public static final String ENTITY_NOT_FOUND_BY_ID = "Không tìm thấy thực thể với ID: %s";

    // ==================== SUCCESS MESSAGES ====================
    public static final String CREATE_SUCCESS = "Tạo thành công";
    public static final String UPDATE_SUCCESS = "Cập nhật thành công";
    public static final String DELETE_SUCCESS = "Xóa thành công";
    public static final String SAVE_SUCCESS = "Lưu thành công";
    public static final String ASSIGN_SUCCESS = "Phân công thành công";
    public static final String CANCEL_SUCCESS = "Hủy thành công";
    public static final String COMPLETE_SUCCESS = "Hoàn thành thành công";
    public static final String RESOLVE_SUCCESS = "Giải quyết thành công";

    /**
     * Format message với các tham số
     * @param template Template message với placeholder %s
     * @param args Các tham số để thay thế
     * @return Message đã được format
     */
    public static String format(String template, Object... args) {
        return String.format(template, args);
    }
}
