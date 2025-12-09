package capstone_project.dtos.response.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardSummaryResponse {
    private String period; // "week", "month", "year"
    private DateRange currentRange;
    private DateRange previousRange;
    private UserTotals totals;
    private FleetStatus fleetStatus;
    private PenaltiesSummary penaltiesSummary;
    private PenaltiesTimeSeries penaltiesTimeSeries;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DateRange {
        private LocalDateTime from;
        private LocalDateTime to;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserTotals {
        private RoleCount customers;
        private RoleCount staff;
        private RoleCount drivers;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoleCount {
        private Long count;
        private Double deltaPercent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MaintenanceAlert {
        private String vehicleId;
        private String licensePlate;
        private String maintenanceType;
        private String scheduledDate;
        private Boolean isOverdue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FleetStatus {
        private Long totalVehicles;
        private Long availableVehicles;
        private Long inUseVehicles;
        private Long inMaintenanceVehicles;
        private List<MaintenanceAlert> maintenanceAlerts;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PenaltiesSummary {
        private Long totalPenalties;
        private Long previousPenalties;
        private Double deltaPercent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PenaltiesTimeSeries {
        private String period;
        private List<DataPoint> points;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataPoint {
        private LocalDate date;
        private Long count;
    }
}
