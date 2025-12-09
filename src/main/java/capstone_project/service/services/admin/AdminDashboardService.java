package capstone_project.service.services.admin;

import capstone_project.dtos.response.admin.AdminDashboardSummaryResponse;
import capstone_project.dtos.response.admin.FleetStatsResponse;
import capstone_project.dtos.response.admin.RegistrationTimeSeriesResponse;
import capstone_project.dtos.response.admin.TopDriverResponse;
import capstone_project.dtos.response.admin.TopStaffResponse;

import java.util.List;

public interface AdminDashboardService {
    AdminDashboardSummaryResponse getDashboardSummary(String period);
    
    RegistrationTimeSeriesResponse getRegistrationTimeSeries(String role, String period);
    
    List<TopStaffResponse> getTopStaff(Integer limit, String period);
    
    List<TopDriverResponse> getTopDrivers(Integer limit, String period);
    
    String getAdminAiSummary(String period);
}
