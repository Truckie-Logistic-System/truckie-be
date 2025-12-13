package capstone_project.dtos.response.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FleetStatsResponse {
    private RoleCount vehicles;
    private RoleCount devices;
    private RoleCount maintenances;
    private RoleCount penalties;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoleCount {
        private long count;
        private double deltaPercent;
    }
}
