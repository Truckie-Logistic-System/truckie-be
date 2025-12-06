package capstone_project.config.order;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

/**
 * Configuration for order cancellation reasons.
 * Reasons are loaded from application.properties and can be updated without code changes.
 */
@Configuration
@ConfigurationProperties(prefix = "order.cancellation.reasons")
@Getter
@Setter
public class OrderCancellationConfig {
    
    /**
     * Comma-separated list of staff cancellation reasons
     */
    private String staff;
    
    /**
     * Get list of staff cancellation reasons
     * @return List of cancellation reasons for staff
     */
    public List<String> getStaffReasons() {
        if (staff == null || staff.isEmpty()) {
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
