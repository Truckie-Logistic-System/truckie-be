package capstone_project.dtos.request.demo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Request DTO for generating demo dashboard data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateDemoDataRequest {
    
    /**
     * Year to generate data for
     */
    @NotNull(message = "Year is required")
    @Min(value = 2020, message = "Year must be >= 2020")
    @Max(value = 2030, message = "Year must be <= 2030")
    private Integer year;
    
    /**
     * Minimum records per month for each entity type
     */
    @Min(value = 1, message = "minPerMonth must be >= 1")
    @Max(value = 50, message = "minPerMonth must be <= 50")
    private Integer minPerMonth = 5;
    
    /**
     * Maximum records per month for each entity type
     */
    @Min(value = 1, message = "maxPerMonth must be >= 1")
    @Max(value = 100, message = "maxPerMonth must be <= 100")
    private Integer maxPerMonth = 20;
    
    /**
     * Configuration for which dashboards to include
     */
    @Builder.Default
    private IncludeConfig include = new IncludeConfig();
    
    /**
     * Optional: Generate data for specific customer ID (if provided, will create orders for this customer)
     */
    private UUID targetCustomerId;
    
    /**
     * Optional: Generate data for specific driver ID (if provided, will create assignments for this driver)
     */
    private UUID targetDriverId;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IncludeConfig {
        @Builder.Default
        private Boolean admin = true;
        
        @Builder.Default
        private Boolean staff = true;
        
        @Builder.Default
        private Boolean customer = true;
        
        @Builder.Default
        private Boolean driver = true;
    }
}
