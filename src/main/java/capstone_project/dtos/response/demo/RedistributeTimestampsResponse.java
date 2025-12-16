package capstone_project.dtos.response.demo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Response DTO for timestamp redistribution operation
 * Provides detailed statistics about the redistribution process
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedistributeTimestampsResponse {
    
    /**
     * Total number of records updated
     */
    private Integer totalRecordsUpdated;
    
    /**
     * Breakdown of updated records by entity type
     */
    private Map<String, Integer> entitiesUpdated;
    
    /**
     * Distribution summary by period
     */
    private DistributionSummary distributionSummary;
    
    /**
     * Execution time in milliseconds
     */
    private Long executionTimeMs;
    
    /**
     * Target month and year
     */
    private Integer targetMonth;
    private Integer targetYear;
    
    /**
     * Distribution strategy used
     */
    private String distributionStrategy;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DistributionSummary {
        private PeriodStats beforeDemoWeek;
        private PeriodStats demoWeek;
        private PeriodStats afterDemoWeek;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PeriodStats {
        private String periodLabel;
        private String startDate;
        private String endDate;
        private Integer recordCount;
        private Double percentage;
        private Integer dailyAverage;
    }
}
