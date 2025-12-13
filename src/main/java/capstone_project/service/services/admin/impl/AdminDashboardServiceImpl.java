package capstone_project.service.services.admin.impl;

import capstone_project.dtos.response.admin.AdminDashboardSummaryResponse;
import capstone_project.dtos.response.admin.FleetStatsResponse;
import capstone_project.dtos.response.admin.RegistrationTimeSeriesResponse;
import capstone_project.dtos.response.admin.TopDriverResponse;
import capstone_project.dtos.response.admin.TopStaffResponse;
import capstone_project.entity.auth.UserEntity;
import capstone_project.entity.user.driver.PenaltyHistoryEntity;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.repository.repositories.auth.UserRepository;
import capstone_project.repository.repositories.issue.IssueRepository;
import capstone_project.repository.repositories.user.PenaltyHistoryRepository;
import capstone_project.repository.repositories.device.DeviceRepository;
import capstone_project.repository.repositories.vehicle.VehicleRepository;
import capstone_project.repository.repositories.vehicle.VehicleAssignmentRepository;
import capstone_project.repository.repositories.vehicle.VehicleServiceRecordRepository;
import capstone_project.service.services.admin.AdminDashboardService;
import capstone_project.service.services.ai.GeminiService;
import capstone_project.service.services.ai.GeminiService.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final UserRepository userRepository;
    private final IssueRepository issueRepository;
    private final VehicleAssignmentRepository vehicleAssignmentRepository;
    private final VehicleRepository vehicleRepository;
    private final DeviceRepository deviceRepository;
    private final VehicleServiceRecordRepository vehicleServiceRecordRepository;
    private final PenaltyHistoryRepository penaltyHistoryRepository;
    private final GeminiService geminiService;

    @Override
    public AdminDashboardSummaryResponse getDashboardSummary(String period) {
        log.info("Getting admin dashboard summary for period: {}", period);
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime currentStart, currentEnd, previousStart, previousEnd;
        
        switch (period.toLowerCase()) {
            case "week":
                currentEnd = now;
                currentStart = now.minusWeeks(1);
                previousEnd = currentStart;
                previousStart = currentStart.minusWeeks(1);
                break;
            case "year":
                currentEnd = now;
                currentStart = now.minusYears(1);
                previousEnd = currentStart;
                previousStart = currentStart.minusYears(1);
                break;
            case "month":
            default:
                currentEnd = now;
                currentStart = now.minusMonths(1);
                previousEnd = currentStart;
                previousStart = currentStart.minusMonths(1);
                break;
        }

        // Count users by role for current and previous periods
        Long currentCustomers = userRepository.countByRoleAndCreatedAtBetween("customer", currentStart, currentEnd);
        Long previousCustomers = userRepository.countByRoleAndCreatedAtBetween("customer", previousStart, previousEnd);
        
        Long currentStaff = userRepository.countByRoleAndCreatedAtBetween("staff", currentStart, currentEnd);
        Long previousStaff = userRepository.countByRoleAndCreatedAtBetween("staff", previousStart, previousEnd);
        
        Long currentDrivers = userRepository.countByRoleAndCreatedAtBetween("driver", currentStart, currentEnd);
        Long previousDrivers = userRepository.countByRoleAndCreatedAtBetween("driver", previousStart, previousEnd);

        // Get fleet status data
        AdminDashboardSummaryResponse.FleetStatus fleetStatus = getFleetStatusData(currentStart, currentEnd);

        // Get penalties summary data
        AdminDashboardSummaryResponse.PenaltiesSummary penaltiesSummary = getPenaltiesSummaryData(currentStart, currentEnd);
        
        // Get penalties time series data
        AdminDashboardSummaryResponse.PenaltiesTimeSeries penaltiesTimeSeries = getPenaltiesTimeSeriesData(period, currentStart, currentEnd);

        return AdminDashboardSummaryResponse.builder()
                .period(period)
                .currentRange(AdminDashboardSummaryResponse.DateRange.builder()
                        .from(currentStart)
                        .to(currentEnd)
                        .build())
                .previousRange(AdminDashboardSummaryResponse.DateRange.builder()
                        .from(previousStart)
                        .to(previousEnd)
                        .build())
                .totals(AdminDashboardSummaryResponse.UserTotals.builder()
                        .customers(AdminDashboardSummaryResponse.RoleCount.builder()
                                .count(currentCustomers)
                                .deltaPercent(calculateDeltaPercent(currentCustomers, previousCustomers))
                                .build())
                        .staff(AdminDashboardSummaryResponse.RoleCount.builder()
                                .count(currentStaff)
                                .deltaPercent(calculateDeltaPercent(currentStaff, previousStaff))
                                .build())
                        .drivers(AdminDashboardSummaryResponse.RoleCount.builder()
                                .count(currentDrivers)
                                .deltaPercent(calculateDeltaPercent(currentDrivers, previousDrivers))
                                .build())
                        .build())
                .fleetStatus(fleetStatus)
                .penaltiesSummary(penaltiesSummary)
                .penaltiesTimeSeries(penaltiesTimeSeries)
                .build();
    }

    @Override
    public RegistrationTimeSeriesResponse getRegistrationTimeSeries(String role, String period) {
        log.info("Getting registration time series for role: {}, period: {}", role, period);
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate;
        boolean groupByMonth = false;
        
        switch (period.toLowerCase()) {
            case "week":
                startDate = now.minusWeeks(1);
                break;
            case "year":
                startDate = now.minusYears(1);
                groupByMonth = true; // Group by month for year view
                break;
            case "month":
            default:
                startDate = now.minusMonths(1);
                break;
        }

        List<UserEntity> users = userRepository.findByRoleAndCreatedAtBetween(role.toLowerCase(), startDate, now);
        
        List<RegistrationTimeSeriesResponse.DataPoint> points;
        
        if (groupByMonth) {
            // Group by month for year view
            Map<String, Long> countByMonth = users.stream()
                    .collect(Collectors.groupingBy(
                            user -> user.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM")),
                            Collectors.counting()
                    ));

            // Fill in missing months with 0
            points = new ArrayList<>();
            LocalDate current = startDate.toLocalDate().withDayOfMonth(1);
            LocalDate end = now.toLocalDate().withDayOfMonth(1);
            
            while (!current.isAfter(end)) {
                String monthKey = current.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
                points.add(RegistrationTimeSeriesResponse.DataPoint.builder()
                        .date(current)
                        .count(countByMonth.getOrDefault(monthKey, 0L))
                        .build());
                current = current.plusMonths(1);
            }
        } else {
            // Group by date for week/month view
            Map<LocalDate, Long> countByDate = users.stream()
                    .collect(Collectors.groupingBy(
                            user -> user.getCreatedAt().toLocalDate(),
                            Collectors.counting()
                    ));

            // Fill in missing dates with 0
            points = new ArrayList<>();
            LocalDate current = startDate.toLocalDate();
            LocalDate end = now.toLocalDate();
            
            while (!current.isAfter(end)) {
                points.add(RegistrationTimeSeriesResponse.DataPoint.builder()
                        .date(current)
                        .count(countByDate.getOrDefault(current, 0L))
                        .build());
                current = current.plusDays(1);
            }
        }

        return RegistrationTimeSeriesResponse.builder()
                .role(role)
                .period(period)
                .points(points)
                .build();
    }

    @Override
    public List<TopStaffResponse> getTopStaff(Integer limit, String period) {
        log.info("Getting top {} staff for period: {}", limit, period);
        
        LocalDateTime startDate = getStartDateForPeriod(period);
        LocalDateTime endDate = LocalDateTime.now();
        
        // Get all staff users
        List<UserEntity> staffUsers = userRepository.findByRole("staff");
        
        // Count resolved issues for each staff (RESOLVED + RESOLVED_SAFE)
        List<TopStaffResponse> topStaff = staffUsers.stream()
                .map(staff -> {
                    Long resolvedCount = issueRepository.countByAssignedToIdAndStatusAndUpdatedAtBetween(
                            staff.getId(), "RESOLVED", startDate, endDate);
                    Long resolvedSafeCount = issueRepository.countByAssignedToIdAndStatusAndUpdatedAtBetween(
                            staff.getId(), "RESOLVED_SAFE", startDate, endDate);
                    Long totalResolved = resolvedCount + resolvedSafeCount;
                    
                    return TopStaffResponse.builder()
                            .staffId(staff.getId())
                            .name(staff.getFullName())
                            .email(staff.getEmail())
                            .resolvedIssues(totalResolved)
                            .avatarUrl(staff.getImageUrl())
                            .build();
                })
                .filter(staff -> staff.getResolvedIssues() > 0)
                .sorted((a, b) -> Long.compare(b.getResolvedIssues(), a.getResolvedIssues()))
                .limit(limit != null ? limit : 5)
                .collect(Collectors.toList());
        
        return topStaff;
    }

    @Override
    public List<TopDriverResponse> getTopDrivers(Integer limit, String period) {
        log.info("Getting top {} drivers for period: {}", limit, period);
        
        LocalDateTime startDate = getStartDateForPeriod(period);
        LocalDateTime endDate = LocalDateTime.now();
        
        try {
            // Get all vehicle assignments in date range (created during the period)
            List<VehicleAssignmentEntity> assignments = vehicleAssignmentRepository.findByCreatedAtBetween(startDate, endDate);
            
            // Group assignments by driver (using driver1 as primary driver)
            Map<UUID, List<VehicleAssignmentEntity>> assignmentsByDriver = assignments.stream()
                    .filter(a -> a.getDriver1() != null)
                    .collect(Collectors.groupingBy(a -> a.getDriver1().getId()));
            
            return assignmentsByDriver.entrySet().stream()
                    .map(e -> {
                        UUID driverId = e.getKey();
                        List<VehicleAssignmentEntity> driverAssignments = e.getValue();
                        
                        // Get driver info from first assignment
                        VehicleAssignmentEntity firstAssignment = driverAssignments.get(0);
                        UserEntity driverUser = firstAssignment.getDriver1().getUser();
                        
                        String driverName = driverUser != null ? driverUser.getFullName() : "";
                        String email = driverUser != null ? driverUser.getEmail() : "";
                        String avatarUrl = driverUser != null ? driverUser.getImageUrl() : "";
                        
                        // Calculate completed trips
                        long completedTrips = driverAssignments.stream()
                                .filter(a -> "COMPLETED".equals(a.getStatus()))
                                .count();
                        
                        return TopDriverResponse.builder()
                                .driverId(driverId)
                                .name(driverName)
                                .email(email)
                                .acceptedTrips(completedTrips)
                                .avatarUrl(avatarUrl)
                                .build();
                    })
                    .filter(driver -> driver.getAcceptedTrips() > 0)
                    .sorted(Comparator.comparing(TopDriverResponse::getAcceptedTrips).reversed())
                    .limit(limit != null ? limit : 5)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.warn("Failed to get top drivers: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private Double calculateDeltaPercent(Long current, Long previous) {
        if (previous == null || previous == 0) {
            return current > 0 ? 100.0 : 0.0;
        }
        return ((current - previous) * 100.0) / previous;
    }

    private LocalDateTime getStartDateForPeriod(String period) {
        LocalDateTime now = LocalDateTime.now();
        
        switch (period.toLowerCase()) {
            case "week":
                return now.minusWeeks(1);
            case "month":
                return now.minusMonths(1);
            case "year":
                return now.minusYears(1);
            default:
                return now.minusMonths(1);
        }
    }

    @Override
    public String getAdminAiSummary(String period) {
        log.info("[Admin AI Summary] Generating summary for period: {}", period);
        
        LocalDateTime startDate = getStartDateForPeriod(period);
        LocalDateTime endDate = LocalDateTime.now();
        String periodLabel = getPeriodLabel(period);
        
        AdminDashboardSummaryResponse summary = null;
        List<TopStaffResponse> topStaff = null;
        List<TopDriverResponse> topDrivers = null;
        
        try {
            // Get all dashboard data for analysis
            summary = getDashboardSummary(period);
            topStaff = getTopStaff(5, period);
            topDrivers = getTopDrivers(5, period);
            
            // Generate AI Summary with fallback
            return generateAdminAiSummary(summary, topStaff, topDrivers, periodLabel);
        } catch (Exception e) {
            log.error("[Admin AI Summary] Error generating AI summary, using fallback", e);
            
            // Fallback summary (simple, no AI)
            StringBuilder fallback = new StringBuilder();
            fallback.append(String.format("📊 **Hệ thống**: %s có tổng số người dùng", periodLabel));
            
            if (summary != null) {
                long totalUsers = summary.getTotals().getCustomers().getCount() + 
                                 summary.getTotals().getStaff().getCount() + 
                                 summary.getTotals().getDrivers().getCount();
                fallback.append(String.format(" **%d** (", totalUsers));
                
                List<String> userStats = new ArrayList<>();
                if (summary.getTotals().getCustomers().getCount() > 0) userStats.add(summary.getTotals().getCustomers().getCount() + " khách hàng");
                if (summary.getTotals().getStaff().getCount() > 0) userStats.add(summary.getTotals().getStaff().getCount() + " nhân viên");
                if (summary.getTotals().getDrivers().getCount() > 0) userStats.add(summary.getTotals().getDrivers().getCount() + " tài xế");
                
                fallback.append(String.join(", ", userStats)).append("). ");
                
                // Registration trends
                if (summary.getTotals().getCustomers().getDeltaPercent() > 0) {
                    fallback.append(String.format("📈 **Tăng trưởng**: Khách hàng +%s%%, ", 
                            summary.getTotals().getCustomers().getDeltaPercent().toString()));
                }
                if (summary.getTotals().getStaff().getDeltaPercent() > 0) {
                    fallback.append(String.format("Nhân viên +%s%%, ", 
                            summary.getTotals().getStaff().getDeltaPercent().toString()));
                }
                if (summary.getTotals().getDrivers().getDeltaPercent() > 0) {
                    fallback.append(String.format("Tài xế +%s%%", 
                            summary.getTotals().getDrivers().getDeltaPercent().toString()));
                }
                fallback.append(". ");
            }
            
            // Top performers
            if (topStaff != null && !topStaff.isEmpty()) {
                fallback.append(String.format("🏆 **Nhân viên xuất sắc**: %s (%d sự cố đã xử lý). ", 
                        topStaff.get(0).getName(), topStaff.get(0).getResolvedIssues()));
            }
            
            if (topDrivers != null && !topDrivers.isEmpty()) {
                fallback.append(String.format("🚛 **Tài xế xuất sắc**: %s (%d chuyến hoàn thành). ", 
                        topDrivers.get(0).getName(), topDrivers.get(0).getAcceptedTrips()));
            }
            
            return fallback.toString();
        }
    }
    
    private String generateAdminAiSummary(
            AdminDashboardSummaryResponse summary,
            List<TopStaffResponse> topStaff,
            List<TopDriverResponse> topDrivers,
            String periodLabel) {
        
        // Fallback summary (hiện đang dùng) phòng khi Gemini lỗi hoặc không cấu hình
        StringBuilder fallback = new StringBuilder();
        fallback.append(String.format("📊 **Hệ thống**: %s có **%d người dùng** (", 
                periodLabel, 
                (summary.getTotals().getCustomers().getCount() + 
                 summary.getTotals().getStaff().getCount() + 
                 summary.getTotals().getDrivers().getCount())));
        
        List<String> userStats = new ArrayList<>();
        if (summary.getTotals().getCustomers().getCount() > 0) userStats.add(summary.getTotals().getCustomers().getCount() + " khách hàng");
        if (summary.getTotals().getStaff().getCount() > 0) userStats.add(summary.getTotals().getStaff().getCount() + " nhân viên");
        if (summary.getTotals().getDrivers().getCount() > 0) userStats.add(summary.getTotals().getDrivers().getCount() + " tài xế");
        
        fallback.append(String.join(", ", userStats)).append("). ");
        
        // Gọi Gemini để tạo tóm tắt giàu ngữ cảnh hơn
        try {
            String systemPrompt = "Bạn là trợ lý phân tích cho quản trị viên hệ thống quản lý vận tải. " +
                    "Hãy tóm tắt ngắn gọn (4-6 câu) tình hình tổng thể hệ thống trong " + periodLabel +
                    " bằng tiếng Việt, giọng chuyên nghiệp, tập trung vào hiệu quả hệ thống và các vấn đề cần quan tâm. " +
                    "Phải phân tích TẤT CẢ các số liệu sau: tổng số người dùng (khách hàng/nhân viên/tài xế), tỷ lệ tăng trưởng, " +
                    "hiệu suất nhân viên (sự cố đã xử lý), hiệu suất tài xế (chuyến hoàn thành), " +
                    "VÀ QUAN TRỌNG NHẤT: các xu hướng tăng trưởng và vấn đề cần ưu tiên. " +
                    "Nếu có tăng trưởng tốt, hãy ghi nhận thành tích. " +
                    "Nếu có hiệu suất thấp hoặc vấn đề, hãy nhấn mạnh và đề xuất hành động. " +
                    "QUAN TRỌNG: Sử dụng định dạng **in đậm** cho tất cả các số liệu quan trọng, tỷ lệ phần trăm, và những điểm cần chú ý đặc biệt.";

            StringBuilder userContext = new StringBuilder();
            userContext.append("=== PHÂN TÍCH DỮ LIỆU DASHBOARD QUẢN TRỊ VIÊN ===\n");
            userContext.append("=== TỔNG QUAN HỆ THỐNG ===\n");
            
            long totalUsers = summary.getTotals().getCustomers().getCount() + 
                             summary.getTotals().getStaff().getCount() + 
                             summary.getTotals().getDrivers().getCount();
            userContext.append(String.format("• Tổng số người dùng: %d\n", totalUsers));
            userContext.append(String.format("• Khách hàng: %d (tăng trưởng: %s%%)\n", 
                    summary.getTotals().getCustomers().getCount(), 
                    summary.getTotals().getCustomers().getDeltaPercent() != null ? 
                            summary.getTotals().getCustomers().getDeltaPercent().toString() : "0"));
            userContext.append(String.format("• Nhân viên: %d (tăng trưởng: %s%%)\n", 
                    summary.getTotals().getStaff().getCount(),
                    summary.getTotals().getStaff().getDeltaPercent() != null ? 
                            summary.getTotals().getStaff().getDeltaPercent().toString() : "0"));
            userContext.append(String.format("• Tài xế: %d (tăng trưởng: %s%%)\n", 
                    summary.getTotals().getDrivers().getCount(),
                    summary.getTotals().getDrivers().getDeltaPercent() != null ? 
                            summary.getTotals().getDrivers().getDeltaPercent().toString() : "0"));
            
            userContext.append("=== HIỆU SUẤT NHÂN VIÊN ===\n");
            if (!topStaff.isEmpty()) {
                userContext.append("• Top 5 nhân viên xuất sắc:\n");
                for (int i = 0; i < Math.min(topStaff.size(), 5); i++) {
                    TopStaffResponse staff = topStaff.get(i);
                    userContext.append(String.format("  %d. %s: %d sự cố đã xử lý\n", 
                            i + 1, staff.getName(), staff.getResolvedIssues()));
                }
            } else {
                userContext.append("• Không có nhân viên nào xử lý sự cố trong kỳ này\n");
            }
            
            userContext.append("=== HIỆU SUẤT TÀI XÉ ===\n");
            if (!topDrivers.isEmpty()) {
                userContext.append("• Top 5 tài xế xuất sắc:\n");
                for (int i = 0; i < Math.min(topDrivers.size(), 5); i++) {
                    TopDriverResponse driver = topDrivers.get(i);
                    userContext.append(String.format("  %d. %s: %d chuyến hoàn thành\n", 
                            i + 1, driver.getName(), driver.getAcceptedTrips()));
                }
            } else {
                userContext.append("• Không có tài xế nào hoàn thành chuyến trong kỳ này\n");
            }
            
            userContext.append("=== PHÂN TÍCH XU HƯỚNG ===\n");
            double customerGrowth = summary.getTotals().getCustomers().getDeltaPercent() != null ? 
                    summary.getTotals().getCustomers().getDeltaPercent() : 0.0;
            double staffGrowth = summary.getTotals().getStaff().getDeltaPercent() != null ? 
                    summary.getTotals().getStaff().getDeltaPercent() : 0.0;
            double driverGrowth = summary.getTotals().getDrivers().getDeltaPercent() != null ? 
                    summary.getTotals().getDrivers().getDeltaPercent() : 0.0;
            
            userContext.append(String.format("• Tăng trưởng khách hàng: %s%%\n", 
                    customerGrowth >= 0 ? "+" + customerGrowth : customerGrowth));
            userContext.append(String.format("• Tăng trưởng nhân viên: %s%%\n", 
                    staffGrowth >= 0 ? "+" + staffGrowth : staffGrowth));
            userContext.append(String.format("• Tăng trưởng tài xế: %s%%\n", 
                    driverGrowth >= 0 ? "+" + driverGrowth : driverGrowth));
            
            // Log the exact context being sent to AI for debugging
            log.info("[Admin AI Summary] Sending to AI:\n{}", userContext.toString());

            List<ChatMessage> messages = List.of(
                    new ChatMessage("user", userContext.toString())
            );

            String geminiSummary = geminiService.generateResponse(systemPrompt, messages);
            if (geminiSummary != null && !geminiSummary.isBlank()) {
                log.info("[Admin AI Summary] AI Response: {}", geminiSummary);
                return geminiSummary;
            } else {
                log.warn("[Admin AI Summary] AI returned empty response, using fallback");
                return fallback.toString();
            }
        } catch (Exception e) {
            log.error("[Admin AI Summary] Error calling Gemini API: {}", e.getMessage(), e);
            return fallback.toString();
        }
    }
    
    private String getPeriodLabel(String period) {
        switch (period.toLowerCase()) {
            case "week":
                return "tuần này";
            case "month":
                return "tháng này";
            case "year":
                return "năm nay";
            default:
                return "tháng này";
        }
    }
    
    private AdminDashboardSummaryResponse.FleetStatus getFleetStatusData(LocalDateTime start, LocalDateTime end) {
        // Count vehicles by status using existing repository methods
        long totalVehicles = vehicleRepository.count();
        long availableVehicles = vehicleRepository.getVehicleEntitiesByVehicleTypeEntityAndStatus(null, "AVAILABLE").size();
        long inUseVehicles = vehicleRepository.getVehicleEntitiesByVehicleTypeEntityAndStatus(null, "IN_USE").size();
        long inMaintenanceVehicles = vehicleRepository.getVehicleEntitiesByVehicleTypeEntityAndStatus(null, "MAINTENANCE").size();
        
        // Get maintenance alerts (vehicles due for maintenance)
        List<AdminDashboardSummaryResponse.MaintenanceAlert> maintenanceAlerts = getMaintenanceAlerts();
        
        return AdminDashboardSummaryResponse.FleetStatus.builder()
                .totalVehicles(totalVehicles)
                .availableVehicles(availableVehicles)
                .inUseVehicles(inUseVehicles)
                .inMaintenanceVehicles(inMaintenanceVehicles)
                .maintenanceAlerts(maintenanceAlerts)
                .build();
    }
    
    private List<AdminDashboardSummaryResponse.MaintenanceAlert> getMaintenanceAlerts() {
        // This would typically query for vehicles due for maintenance
        // For now, returning empty list - you can implement the actual logic
        return new ArrayList<>();
    }
    
    private AdminDashboardSummaryResponse.PenaltiesSummary getPenaltiesSummaryData(LocalDateTime start, LocalDateTime end) {
        // Count penalties for current and previous periods
        LocalDateTime previousStart = start.minus(java.time.Duration.between(start, end));
        LocalDateTime previousEnd = start;
        
        long currentPenalties = penaltyHistoryRepository.countByCreatedAtBetween(start, end);
        long previousPenalties = penaltyHistoryRepository.countByCreatedAtBetween(previousStart, previousEnd);
        
        return AdminDashboardSummaryResponse.PenaltiesSummary.builder()
                .totalPenalties(currentPenalties)
                .previousPenalties(previousPenalties)
                .deltaPercent(calculateDeltaPercent(currentPenalties, previousPenalties))
                .build();
    }
    
    private AdminDashboardSummaryResponse.PenaltiesTimeSeries getPenaltiesTimeSeriesData(String period, LocalDateTime start, LocalDateTime end) {
        List<PenaltyHistoryEntity> penalties = penaltyHistoryRepository.findByCreatedAtBetween(start, end);
        
        List<AdminDashboardSummaryResponse.DataPoint> points;
        
        if (period.equalsIgnoreCase("year")) {
            // Group by month for year view
            Map<String, Long> countByMonth = penalties.stream()
                    .collect(Collectors.groupingBy(
                            penalty -> penalty.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM")),
                            Collectors.counting()
                    ));

            // Fill in missing months with 0
            points = new ArrayList<>();
            LocalDate current = start.toLocalDate().withDayOfMonth(1);
            LocalDate endMonth = end.toLocalDate().withDayOfMonth(1);
            
            while (!current.isAfter(endMonth)) {
                String monthKey = current.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
                points.add(AdminDashboardSummaryResponse.DataPoint.builder()
                        .date(current)
                        .count(countByMonth.getOrDefault(monthKey, 0L))
                        .build());
                current = current.plusMonths(1);
            }
        } else {
            // Group by date for week/month view
            Map<LocalDate, Long> countByDate = penalties.stream()
                    .collect(Collectors.groupingBy(
                            penalty -> penalty.getCreatedAt().toLocalDate(),
                            Collectors.counting()
                    ));

            // Fill in missing dates with 0
            points = new ArrayList<>();
            LocalDate current = start.toLocalDate();
            LocalDate endDate = end.toLocalDate();
            
            while (!current.isAfter(endDate)) {
                points.add(AdminDashboardSummaryResponse.DataPoint.builder()
                        .date(current)
                        .count(countByDate.getOrDefault(current, 0L))
                        .build());
                current = current.plusDays(1);
            }
        }

        return AdminDashboardSummaryResponse.PenaltiesTimeSeries.builder()
                .period(period)
                .points(points)
                .build();
    }
}
