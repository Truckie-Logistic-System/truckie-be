package capstone_project.dtos.request.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardFilterRequest {
    
    public enum TimeRange {
        WEEK,
        MONTH,
        YEAR,
        CUSTOM
    }
    
    @Builder.Default
    private TimeRange range = TimeRange.WEEK;
    private LocalDateTime fromDate;
    private LocalDateTime toDate;
    
    /**
     * Get start date based on time range
     */
    public LocalDateTime getStartDate() {
        if (range == TimeRange.CUSTOM && fromDate != null) {
            return fromDate;
        }
        
        LocalDateTime now = LocalDateTime.now();
        return switch (range) {
            case WEEK -> now.minusDays(7).toLocalDate().atStartOfDay();
            case MONTH -> now.minusMonths(1).toLocalDate().atStartOfDay();
            case YEAR -> now.minusYears(1).toLocalDate().atStartOfDay();
            case CUSTOM -> fromDate != null ? fromDate : now.minusMonths(1).toLocalDate().atStartOfDay();
        };
    }
    
    /**
     * Get end date based on time range
     */
    public LocalDateTime getEndDate() {
        if (range == TimeRange.CUSTOM && toDate != null) {
            return toDate;
        }
        return LocalDateTime.now();
    }
}
