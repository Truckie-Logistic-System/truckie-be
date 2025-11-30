package capstone_project.dtos.response.notification;

import capstone_project.common.enums.NotificationTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationStatsResponse {
    private long totalCount;
    private long unreadCount;
    private long readCount;
    private Map<NotificationTypeEnum, Long> countByType;
}
