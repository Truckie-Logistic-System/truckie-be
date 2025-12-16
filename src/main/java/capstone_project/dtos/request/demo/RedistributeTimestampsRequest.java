package capstone_project.dtos.request.demo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request DTO for redistributing timestamps of demo data
 * Supports flexible distribution strategies for dashboard visualization testing
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedistributeTimestampsRequest {
    
    /**
     * Target month (1-12)
     */
    @NotNull(message = "Target month is required")
    @Min(value = 1, message = "Month must be between 1 and 12")
    @Max(value = 12, message = "Month must be between 1 and 12")
    private Integer targetMonth;
    
    /**
     * Target year (e.g., 2025)
     */
    @NotNull(message = "Target year is required")
    @Min(value = 2024, message = "Year must be 2024 or later")
    @Max(value = 2030, message = "Year must be 2030 or earlier")
    private Integer targetYear;
    
    /**
     * Distribution strategy
     * Options: EVEN (evenly distributed), FOCUS_DEMO_WEEK (concentrate on demo week)
     */
    @Builder.Default
    private String distributionStrategy = "FOCUS_DEMO_WEEK";
    
    /**
     * Start date of demo week (focused period)
     */
    private LocalDate demoWeekStart;
    
    /**
     * End date of demo week (focused period)
     */
    private LocalDate demoWeekEnd;
    
    /**
     * Percentage of data before demo week (0-100)
     */
    @Builder.Default
    @Min(value = 0, message = "Percentage must be between 0 and 100")
    @Max(value = 100, message = "Percentage must be between 0 and 100")
    private Integer percentageBeforeDemoWeek = 30;
    
    /**
     * Percentage of data during demo week (0-100)
     */
    @Builder.Default
    @Min(value = 0, message = "Percentage must be between 0 and 100")
    @Max(value = 100, message = "Percentage must be between 0 and 100")
    private Integer percentageDemoWeek = 50;
    
    /**
     * Percentage of data after demo week (0-100)
     */
    @Builder.Default
    @Min(value = 0, message = "Percentage must be between 0 and 100")
    @Max(value = 100, message = "Percentage must be between 0 and 100")
    private Integer percentageAfterDemoWeek = 20;
    
    /**
     * Validate that percentages sum to 100
     */
    @JsonIgnore
    public boolean isValidPercentageDistribution() {
        return (percentageBeforeDemoWeek + percentageDemoWeek + percentageAfterDemoWeek) == 100;
    }
}
