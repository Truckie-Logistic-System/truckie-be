package capstone_project.config.order;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.Arrays;
import java.util.List;

/**
 * Configuration for order cancellation reasons.
 * Reasons are loaded from application.properties and can be updated without code changes.
 */
@Configuration
@PropertySource(value = "classpath:list-config.properties", encoding = "UTF-8")
@ConfigurationProperties(prefix = "order.cancellation.reasons")
@Getter
@Setter
public class OrderCancellationConfig {
    
    /**
     * Comma-separated list of staff cancellation reasons
     */
    private String staff;
    
    /**
     * Get list of staff cancellation reasons.
     *
     * Ưu tiên đọc từ file order-cancellation.properties (UTF-8).
     * Nếu vì lý do gì đó không có cấu hình, fallback về danh sách mặc định
     * định nghĩa trong code (cũng UTF-8).
     */
    public List<String> getStaffReasons() {
        if (staff == null || staff.isBlank()) {
            return List.of(
                "Không đủ điều kiện vận chuyển",
                "Hàng hóa không phù hợp với dịch vụ",
                "Thông tin đơn hàng không chính xác",
                "Địa chỉ giao hàng không hợp lệ",
                "Khách hàng yêu cầu hủy",
                "Không thể liên lạc với khách hàng",
                "Hàng hóa bị cấm vận chuyển",
                "Lý do khác"
            );
        }
        return Arrays.asList(staff.split(","));
    }
}
