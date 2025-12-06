package capstone_project.service.services.dashboard;

import capstone_project.dtos.request.dashboard.DashboardFilterRequest;
import capstone_project.dtos.response.dashboard.role.*;

import java.util.UUID;

public interface RoleDashboardService {
    
    /**
     * Get admin dashboard data with comprehensive system overview
     */
    AdminDashboardResponse getAdminDashboard(DashboardFilterRequest filter);
    
    /**
     * Get admin dashboard AI summary only
     */
    String getAdminAiSummary(DashboardFilterRequest filter);
    
    /**
     * Get staff dashboard data focused on operations and issues
     */
    StaffDashboardResponse getStaffDashboard(DashboardFilterRequest filter);
    
    /**
     * Get staff dashboard AI summary only
     */
    String getStaffAiSummary(DashboardFilterRequest filter);
    
    /**
     * Get customer dashboard data for their orders and activities
     */
    CustomerDashboardResponse getCustomerDashboard(UUID customerId, DashboardFilterRequest filter);
    
    /**
     * Get driver dashboard data for their trips and KPI
     */
    DriverDashboardResponse getDriverDashboard(UUID driverId, DashboardFilterRequest filter);
    
    /**
     * Get customer dashboard AI summary only
     */
    String getCustomerAiSummary(UUID customerId, DashboardFilterRequest filter);
    
    /**
     * Get driver dashboard AI summary only
     */
    String getDriverAiSummary(UUID driverId, DashboardFilterRequest filter);
}
