package capstone_project.dtos.response.notification;

import capstone_project.common.enums.NotificationTypeEnum;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationResponse {
    private UUID id;
    private String recipientRole;
    private String title;
    private String description;
    private NotificationTypeEnum notificationType;
    
    // Related entity IDs
    private UUID relatedOrderId;
    private List<UUID> relatedOrderDetailIds;  // Parsed from JSON string
    private UUID relatedIssueId;
    private UUID relatedVehicleAssignmentId;
    private UUID relatedContractId;
    
    // Metadata (parsed from JSON string)
    private Map<String, Object> metadata;
    
    // Status
    @JsonProperty("isRead")
    private boolean isRead;
    private LocalDateTime readAt;
    
    // Tracking
    @JsonProperty("emailSent")
    private boolean emailSent;
    private LocalDateTime emailSentAt;
    @JsonProperty("pushNotificationSent")
    private boolean pushNotificationSent;
    private LocalDateTime pushNotificationSentAt;
    
    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
