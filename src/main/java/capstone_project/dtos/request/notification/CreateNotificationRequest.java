package capstone_project.dtos.request.notification;

import capstone_project.common.enums.NotificationTypeEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Request DTO để tạo notification
 * Sử dụng bởi internal services (không expose qua API)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateNotificationRequest {
    
    @NotNull(message = "User ID không được để trống")
    private UUID userId;
    
    @NotBlank(message = "Recipient role không được để trống")
    private String recipientRole;  // CUSTOMER, STAFF, DRIVER
    
    @NotBlank(message = "Title không được để trống")
    private String title;
    
    @NotBlank(message = "Description không được để trống")
    private String description;
    
    @NotNull(message = "Notification type không được để trống")
    private NotificationTypeEnum notificationType;
    
    // Related entities (nullable)
    private UUID relatedOrderId;
    private List<UUID> relatedOrderDetailIds;
    private UUID relatedIssueId;
    private UUID relatedVehicleAssignmentId;
    private UUID relatedContractId;
    
    // Metadata (will be converted to JSON)
    private Map<String, Object> metadata;
}
