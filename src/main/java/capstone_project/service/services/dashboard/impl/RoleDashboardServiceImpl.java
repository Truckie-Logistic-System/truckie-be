package capstone_project.service.services.dashboard.impl;

import capstone_project.dtos.request.dashboard.DashboardFilterRequest;
import capstone_project.dtos.response.dashboard.role.*;
import capstone_project.entity.issue.IssueEntity;
import capstone_project.entity.order.contract.ContractEntity;
import capstone_project.entity.order.order.OrderDetailEntity;
import capstone_project.entity.order.order.OrderEntity;
import capstone_project.entity.order.order.RefundEntity;
import capstone_project.entity.order.transaction.TransactionEntity;
import capstone_project.entity.user.driver.PenaltyHistoryEntity;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.entity.vehicle.VehicleEntity;
import capstone_project.entity.vehicle.VehicleServiceRecordEntity;
import capstone_project.repository.entityServices.issue.IssueEntityService;
import capstone_project.repository.entityServices.order.VehicleFuelConsumptionEntityService;
import capstone_project.repository.entityServices.order.contract.ContractEntityService;
import capstone_project.repository.entityServices.order.order.OrderDetailEntityService;
import capstone_project.repository.entityServices.order.order.OrderEntityService;
import capstone_project.repository.entityServices.order.transaction.TransactionEntityService;
import capstone_project.repository.entityServices.user.CustomerEntityService;
import capstone_project.repository.entityServices.user.DriverEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleAssignmentEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleServiceRecordEntityService;
import capstone_project.repository.entityServices.refund.RefundEntityService;
import capstone_project.repository.repositories.issue.IssueRepository;
import capstone_project.repository.repositories.user.PenaltyHistoryRepository;
import capstone_project.service.services.dashboard.RoleDashboardService;
import capstone_project.service.services.ai.GeminiService;
import capstone_project.service.services.ai.GeminiService.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoleDashboardServiceImpl implements RoleDashboardService {

    private final OrderEntityService orderEntityService;
    private final OrderDetailEntityService orderDetailEntityService;
    private final ContractEntityService contractEntityService;
    private final TransactionEntityService transactionEntityService;
    private final IssueEntityService issueEntityService;
    private final VehicleEntityService vehicleEntityService;
    private final VehicleAssignmentEntityService vehicleAssignmentEntityService;
    private final VehicleServiceRecordEntityService vehicleServiceRecordEntityService;
    private final VehicleFuelConsumptionEntityService vehicleFuelConsumptionEntityService;
    private final CustomerEntityService customerEntityService;
    private final DriverEntityService driverEntityService;
    private final RefundEntityService refundEntityService;
    private final GeminiService geminiService;
    private final IssueRepository issueRepository;
    private final PenaltyHistoryRepository penaltyHistoryRepository;
    private final capstone_project.repository.repositories.auth.UserRepository userRepository;
    private final capstone_project.repository.entityServices.device.DeviceEntityService deviceEntityService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DEADLINE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    @Override
    public AdminDashboardResponse getAdminDashboard(DashboardFilterRequest filter) {
        LocalDateTime startDate = filter.getStartDate();
        LocalDateTime endDate = filter.getEndDate();

        log.info("[AdminDashboard] range={}, startDate={}, endDate={}",
                filter.getRange(), startDate, endDate);
        
        log.info("[Admin Dashboard] Getting data from {} to {}", startDate, endDate);

        // Get all orders in date range
        List<OrderEntity> allOrders = orderEntityService.findAll();
        List<OrderEntity> filteredOrders = filterByDateRange(allOrders, startDate, endDate);
        
        // Get all order details
        List<OrderDetailEntity> allOrderDetails = new ArrayList<>();
        for (OrderEntity order : filteredOrders) {
            allOrderDetails.addAll(order.getOrderDetailEntities());
        }
        
        // Get issues
        List<IssueEntity> allIssues = issueEntityService.findAll();
        List<IssueEntity> filteredIssues = allIssues.stream()
                .filter(i -> i.getReportedAt() != null && 
                        !i.getReportedAt().isBefore(startDate) && 
                        !i.getReportedAt().isAfter(endDate))
                .collect(Collectors.toList());
        
        // Get contracts
        List<ContractEntity> allContracts = contractEntityService.findAll();
        
        // Get transactions
        List<TransactionEntity> allTransactions = transactionEntityService.findAll();
        List<TransactionEntity> filteredTransactions = allTransactions.stream()
                .filter(t -> t.getPaymentDate() != null && 
                        !t.getPaymentDate().isBefore(startDate) && 
                        !t.getPaymentDate().isAfter(endDate))
                .collect(Collectors.toList());
        
        // Get refunds
        List<RefundEntity> allRefunds = refundEntityService.findAll();
        List<RefundEntity> filteredRefunds = allRefunds.stream()
                .filter(r -> r.getRefundDate() != null && 
                        !r.getRefundDate().isBefore(startDate) && 
                        !r.getRefundDate().isAfter(endDate))
                .collect(Collectors.toList());
        
        // Get vehicles
        List<VehicleEntity> allVehicles = vehicleEntityService.findAll();
        
        // Get maintenances
        List<VehicleServiceRecordEntity> allMaintenances = vehicleServiceRecordEntityService.findAll();

        // Calculate KPIs
        AdminDashboardResponse.KpiSummary kpiSummary = buildAdminKpiSummary(
                filteredOrders, allOrderDetails, filteredTransactions, filteredIssues, filteredRefunds);
        
        // Order status distribution
        Map<String, Long> orderStatusDistribution = allOrderDetails.stream()
                .filter(od -> od.getStatus() != null)
                .collect(Collectors.groupingBy(OrderDetailEntity::getStatus, Collectors.counting()));
        
        // Build order trend
        List<AdminDashboardResponse.TrendDataPoint> orderTrend = buildOrderTrend(filteredOrders, filter);
        
        // Build revenue trend
        List<AdminDashboardResponse.TrendDataPoint> revenueTrend = buildRevenueTrend(filteredTransactions, filter);
        
        // Delivery performance
        AdminDashboardResponse.DeliveryPerformance deliveryPerformance = buildDeliveryPerformance(allOrderDetails);
        
        // Issue/Refund summary
        AdminDashboardResponse.IssueRefundSummary issueRefundSummary = buildIssueRefundSummary(filteredIssues, filteredRefunds);
        
        // Top customers (using existing service)
        List<AdminDashboardResponse.TopPerformer> topCustomers = buildTopCustomers();
        
        // Top drivers
        List<AdminDashboardResponse.TopPerformer> topDrivers = buildTopDrivers();
        
        // Fleet health - fix inUseVehicles to use IN_TRANSIT status
        AdminDashboardResponse.FleetHealthSummary fleetHealth = buildFleetHealth(allVehicles, allMaintenances);
        
        // Top staff
        List<AdminDashboardResponse.TopPerformer> topStaff = buildTopStaff(filteredIssues);
        
        // Registration data (customer, staff, driver time series)
        AdminDashboardResponse.RegistrationData registrationData = buildRegistrationData(startDate, endDate, filter);
        
        // Device statistics
        AdminDashboardResponse.DeviceStatistics deviceStatistics = buildDeviceStatistics();
        
        // Fuel consumption statistics
        AdminDashboardResponse.FuelConsumptionStatistics fuelConsumptionStatistics = buildFuelConsumptionStatistics(startDate, endDate, filter);
        
        // Penalties statistics
        AdminDashboardResponse.PenaltiesStatistics penaltiesStatistics = buildPenaltiesStatistics(startDate, endDate, filter);
        
        // Vehicle inspection alerts (vehicles due for inspection/maintenance)
        List<AdminDashboardResponse.VehicleInspectionAlert> vehicleInspectionAlerts = buildVehicleInspectionAlerts(allVehicles, allMaintenances);
        
        // AI Summary is now fetched separately via /admin/ai-summary endpoint
        // to avoid blocking the dashboard data response
        
        return AdminDashboardResponse.builder()
                .aiSummary(null) // AI summary loaded separately
                .kpiSummary(kpiSummary)
                .orderTrend(orderTrend)
                .revenueTrend(revenueTrend)
                .deliveryPerformance(deliveryPerformance)
                .issueRefundSummary(issueRefundSummary)
                .topCustomers(topCustomers)
                .topDrivers(topDrivers)
                .topStaff(topStaff)
                .fleetHealth(fleetHealth)
                .deviceStatistics(deviceStatistics)
                .fuelConsumptionStatistics(fuelConsumptionStatistics)
                .penaltiesStatistics(penaltiesStatistics)
                .vehicleInspectionAlerts(vehicleInspectionAlerts)
                .orderStatusDistribution(orderStatusDistribution)
                .registrationData(registrationData)
                .build();
    }

    @Override
    public String getAdminAiSummary(DashboardFilterRequest filter) {
        LocalDateTime startDate = filter.getStartDate();
        LocalDateTime endDate = filter.getEndDate();
        
        log.info("[Admin AI Summary] Generating summary from {} to {}", startDate, endDate);

        // Get all orders in date range
        List<OrderEntity> allOrders = orderEntityService.findAll();
        List<OrderEntity> filteredOrders = filterByDateRange(allOrders, startDate, endDate);
        
        // Get all order details
        List<OrderDetailEntity> allOrderDetails = new ArrayList<>();
        for (OrderEntity order : filteredOrders) {
            allOrderDetails.addAll(order.getOrderDetailEntities());
        }
        
        // Get issues
        List<IssueEntity> allIssues = issueEntityService.findAll();
        List<IssueEntity> filteredIssues = allIssues.stream()
                .filter(i -> i.getReportedAt() != null && 
                        !i.getReportedAt().isBefore(startDate) && 
                        !i.getReportedAt().isAfter(endDate))
                .collect(Collectors.toList());
        
        // Get refunds
        List<RefundEntity> allRefunds = refundEntityService.findAll();
        List<RefundEntity> filteredRefunds = allRefunds.stream()
                .filter(r -> r.getRefundDate() != null && 
                        !r.getRefundDate().isBefore(startDate) && 
                        !r.getRefundDate().isAfter(endDate))
                .collect(Collectors.toList());
        
        // Get vehicles
        List<VehicleEntity> allVehicles = vehicleEntityService.findAll();
        
        // Get maintenances
        List<VehicleServiceRecordEntity> allMaintenances = vehicleServiceRecordEntityService.findAll();

        // Get transactions for filtering
        List<TransactionEntity> allTransactions = transactionEntityService.findAll();
        List<TransactionEntity> filteredTransactions = allTransactions.stream()
                .filter(t -> t.getPaymentDate() != null && 
                        !t.getPaymentDate().isBefore(startDate) && 
                        !t.getPaymentDate().isAfter(endDate))
                .collect(Collectors.toList());
        
        // Build summary data (reuse existing helper methods)
        AdminDashboardResponse.KpiSummary kpiSummary = buildAdminKpiSummary(filteredOrders, allOrderDetails, filteredTransactions, filteredIssues, filteredRefunds);
        AdminDashboardResponse.DeliveryPerformance deliveryPerformance = buildDeliveryPerformance(allOrderDetails);
        AdminDashboardResponse.IssueRefundSummary issueRefundSummary = buildIssueRefundSummary(filteredIssues, filteredRefunds);
        AdminDashboardResponse.FleetHealthSummary fleetHealth = buildFleetHealth(allVehicles, allMaintenances);

        // Generate AI Summary with fallback
        try {
            return generateAdminAiSummary(kpiSummary, deliveryPerformance, issueRefundSummary, fleetHealth, filter);
        } catch (Exception e) {
            log.error("[Admin AI Summary] Error generating AI summary, using fallback", e);
            
            // Fallback summary (simple, no AI)
            StringBuilder fallback = new StringBuilder();
            String periodLabel = getPeriodLabel(filter);
            
            fallback.append(String.format("Trong %s, hệ thống có **%d đơn hàng** với **%d kiện hàng**, ", 
                    periodLabel, kpiSummary.getTotalOrders(), kpiSummary.getTotalOrderDetails()));
            fallback.append(String.format("doanh thu đạt **%s VNĐ**. ", formatCurrency(kpiSummary.getTotalRevenue())));
            fallback.append(String.format("Tỷ lệ giao hàng đúng hẹn: **%.1f%%**. ", deliveryPerformance.getOnTimePercentage()));
            
            if (issueRefundSummary.getOpenIssues() > 0) {
                fallback.append(String.format("Có **%d sự cố** đang mở cần xử lý. ", issueRefundSummary.getOpenIssues()));
            }
            
            if (fleetHealth.getOverdueMaintenanceVehicles() > 0) {
                fallback.append(String.format("**%d xe** đã quá hạn bảo dưỡng. ", fleetHealth.getOverdueMaintenanceVehicles()));
            }
            
            return fallback.toString();
        }
    }

    @Override
    public StaffDashboardResponse getStaffDashboard(DashboardFilterRequest filter) {
        LocalDateTime startDate = filter.getStartDate();
        LocalDateTime endDate = filter.getEndDate();
        
        log.info("[Staff Dashboard] Getting data from {} to {}", startDate, endDate);

        // Get all orders and filter by date range
        List<OrderEntity> allOrders = orderEntityService.findAll();
        List<OrderEntity> filteredOrders = filterByDateRange(allOrders, startDate, endDate);
        
        // Get all order details from filtered orders
        List<OrderDetailEntity> allOrderDetails = new ArrayList<>();
        for (OrderEntity order : filteredOrders) {
            allOrderDetails.addAll(order.getOrderDetailEntities());
        }

        // Get vehicle assignments (trips) and filter by date range
        List<VehicleAssignmentEntity> allAssignments = vehicleAssignmentEntityService.findAll();
        List<VehicleAssignmentEntity> filteredAssignments = allAssignments.stream()
                .filter(a -> a.getCreatedAt() != null && 
                        !a.getCreatedAt().isBefore(startDate) && 
                        !a.getCreatedAt().isAfter(endDate))
                .collect(Collectors.toList());
        
        // Get issues and filter by date range
        List<IssueEntity> allIssues = issueEntityService.findAll();
        List<IssueEntity> filteredIssues = allIssues.stream()
                .filter(i -> i.getReportedAt() != null && 
                        !i.getReportedAt().isBefore(startDate) && 
                        !i.getReportedAt().isAfter(endDate))
                .collect(Collectors.toList());
        
        // Get contracts and filter by date range
        List<ContractEntity> allContracts = contractEntityService.findAll();
        List<ContractEntity> filteredContracts = allContracts.stream()
                .filter(c -> c.getCreatedAt() != null && 
                        !c.getCreatedAt().isBefore(startDate) && 
                        !c.getCreatedAt().isAfter(endDate))
                .collect(Collectors.toList());
        
        // Get transactions and filter by date range
        List<TransactionEntity> allTransactions = transactionEntityService.findAll();
        List<TransactionEntity> filteredTransactions = allTransactions.stream()
                .filter(t -> t.getPaymentDate() != null && 
                        !t.getPaymentDate().isBefore(startDate) && 
                        !t.getPaymentDate().isAfter(endDate))
                .collect(Collectors.toList());
        
        // Get refunds and filter by date range
        List<RefundEntity> allRefunds = refundEntityService.findAll();
        List<RefundEntity> filteredRefunds = allRefunds.stream()
                .filter(r -> r.getRefundDate() != null && 
                        !r.getRefundDate().isBefore(startDate) && 
                        !r.getRefundDate().isAfter(endDate))
                .collect(Collectors.toList());
        
        // Get vehicles (no date filter - current state)
        List<VehicleEntity> allVehicles = vehicleEntityService.findAll();
        
        // Get maintenances (no date filter - for alerts)
        List<VehicleServiceRecordEntity> allMaintenances = vehicleServiceRecordEntityService.findAll();

        // === BUILD RESPONSE DATA ===
        
        // Operational summary
        StaffDashboardResponse.OperationalSummary operationalSummary = buildOperationalSummary(filteredAssignments);
        
        // Trip status distribution (filtered by date)
        Map<String, Long> tripStatusDistribution = filteredAssignments.stream()
                .filter(a -> a.getStatus() != null)
                .collect(Collectors.groupingBy(VehicleAssignmentEntity::getStatus, Collectors.counting()));
        
        // Trip alerts (from all issues with OPEN/IN_PROGRESS status)
        List<StaffDashboardResponse.TripAlert> tripAlerts = buildTripAlerts(allAssignments, allIssues);
        
        // Issue summary
        StaffDashboardResponse.IssueSummary issueSummary = buildIssueSummary(filteredIssues);
        
        // Pending issues (OPEN and IN_PROGRESS only)
        List<StaffDashboardResponse.IssueItem> pendingIssues = buildPendingIssues(allIssues);
        
        // Financial summary
        StaffDashboardResponse.FinancialSummary financialSummary = buildFinancialSummary(filteredContracts, filteredTransactions, filteredRefunds);
        
        // Fleet status
        StaffDashboardResponse.FleetStatus fleetStatus = buildFleetStatus(allVehicles, allMaintenances);
        
        // Driver performance section removed
        
        // === NEW DATA FOR ENHANCED DASHBOARD ===
        
        // Package summary (like customer dashboard)
        StaffDashboardResponse.PackageSummary packageSummary = buildStaffPackageSummary(filteredOrders, allOrderDetails);
        
        // Trip completion trend
        List<StaffDashboardResponse.TripCompletionTrend> tripCompletionTrend = buildTripCompletionTrend(filteredAssignments, filter);
        
        // Issue type trend (multi-line chart by issue type)
        List<StaffDashboardResponse.IssueTypeTrend> issueTypeTrend = buildIssueTypeTrend(filteredIssues, filter);
        
        // Contract trend
        List<StaffDashboardResponse.ContractTrend> contractTrend = buildStaffContractTrend(filteredContracts, filter);
        
        // Transaction trend
        List<StaffDashboardResponse.TransactionTrend> transactionTrend = buildStaffTransactionTrend(filteredTransactions, filter);
        
        // Refund trend
        List<StaffDashboardResponse.RefundTrend> refundTrend = buildRefundTrend(filteredRefunds, filter);
        
        // Revenue vs Compensation trend
        List<StaffDashboardResponse.RevenueCompensationTrend> revenueCompensationTrend = buildRevenueCompensationTrend(filteredTransactions, filteredRefunds, filter);
        
        // Package status trend (like customer dashboard)
        List<StaffDashboardResponse.PackageStatusTrend> packageStatusTrend = buildStaffPackageStatusTrend(allOrderDetails, filter);
        
        // Recent orders
        List<StaffDashboardResponse.RecentOrderItem> recentOrders = buildStaffRecentOrders(filteredOrders);
        
        // Pending orders (PROCESSING, ON_PLANNING)
        List<StaffDashboardResponse.PendingOrderItem> pendingOrders = buildPendingOrders(allOrders);
        
        // Top customers
        List<StaffDashboardResponse.TopCustomerItem> topCustomers = buildTopCustomers(filteredOrders, filteredTransactions);
        
        // Top drivers
        List<StaffDashboardResponse.TopDriverItem> topDrivers = buildTopDrivers(filteredAssignments);

        return StaffDashboardResponse.builder()
                .operationalSummary(operationalSummary)
                .tripStatusDistribution(tripStatusDistribution)
                .tripAlerts(tripAlerts)
                .issueSummary(issueSummary)
                .pendingIssues(pendingIssues)
                .financialSummary(financialSummary)
                .fleetStatus(fleetStatus)
                // Driver performance removed
                // New fields
                .packageSummary(packageSummary)
                .tripCompletionTrend(tripCompletionTrend)
                .issueTypeTrend(issueTypeTrend)
                .contractTrend(contractTrend)
                .transactionTrend(transactionTrend)
                .refundTrend(refundTrend)
                .revenueCompensationTrend(revenueCompensationTrend)
                .packageStatusTrend(packageStatusTrend)
                .recentOrders(recentOrders)
                .pendingOrders(pendingOrders)
                .topCustomers(topCustomers)
                .topDrivers(topDrivers)
                .build();
    }

    @Override
    public String getStaffAiSummary(DashboardFilterRequest filter) {
        LocalDateTime startDate = filter.getStartDate();
        LocalDateTime endDate = filter.getEndDate();
        
        log.info("[Staff AI Summary] Generating summary from {} to {}", startDate, endDate);

        // Get all orders in date range
        List<OrderEntity> allOrders = orderEntityService.findAll();
        List<OrderEntity> filteredOrders = filterByDateRange(allOrders, startDate, endDate);
        
        // Get all order details
        List<OrderDetailEntity> allOrderDetails = new ArrayList<>();
        for (OrderEntity order : filteredOrders) {
            allOrderDetails.addAll(order.getOrderDetailEntities());
        }
        
        // Get issues
        List<IssueEntity> allIssues = issueEntityService.findAll();
        List<IssueEntity> filteredIssues = allIssues.stream()
                .filter(i -> i.getReportedAt() != null && 
                        !i.getReportedAt().isBefore(startDate) && 
                        !i.getReportedAt().isAfter(endDate))
                .collect(Collectors.toList());
        
        // Get contracts
        List<ContractEntity> allContracts = contractEntityService.findAll();
        
        // Get transactions
        List<TransactionEntity> allTransactions = transactionEntityService.findAll();
        List<TransactionEntity> filteredTransactions = allTransactions.stream()
                .filter(t -> t.getPaymentDate() != null && 
                        !t.getPaymentDate().isBefore(startDate) && 
                        !t.getPaymentDate().isAfter(endDate))
                .collect(Collectors.toList());
        
        // Get refunds
        List<RefundEntity> allRefunds = refundEntityService.findAll();
        List<RefundEntity> filteredRefunds = allRefunds.stream()
                .filter(r -> r.getRefundDate() != null && 
                        !r.getRefundDate().isBefore(startDate) && 
                        !r.getRefundDate().isAfter(endDate))
                .collect(Collectors.toList());
        
        // Get vehicle assignments and filter by date
        List<VehicleAssignmentEntity> allAssignments = vehicleAssignmentEntityService.findAll();
        List<VehicleAssignmentEntity> filteredAssignments = allAssignments.stream()
                .filter(a -> a.getCreatedAt() != null && 
                        !a.getCreatedAt().isBefore(startDate) && 
                        !a.getCreatedAt().isAfter(endDate))
                .collect(Collectors.toList());
        
        // Get vehicles
        List<VehicleEntity> allVehicles = vehicleEntityService.findAll();
        
        // Get maintenances
        List<VehicleServiceRecordEntity> allMaintenances = vehicleServiceRecordEntityService.findAll();
        
        // Build summary data (reuse existing helper methods)
        StaffDashboardResponse.OperationalSummary operationalSummary = buildOperationalSummary(filteredAssignments);
        StaffDashboardResponse.IssueSummary issueSummary = buildIssueSummary(filteredIssues);
        StaffDashboardResponse.FinancialSummary financialSummary = buildFinancialSummary(allContracts, filteredTransactions, filteredRefunds);
        StaffDashboardResponse.FleetStatus fleetStatus = buildFleetStatus(allVehicles, allMaintenances);

        // Generate AI Summary with fallback
        try {
            return generateStaffAiSummary(operationalSummary, issueSummary, financialSummary, fleetStatus, filter);
        } catch (Exception e) {
            log.error("[Staff AI Summary] Error generating AI summary, using fallback", e);
            
            // Fallback summary (simple, no AI)
            StringBuilder fallback = new StringBuilder();
            String periodLabel = getPeriodLabel(filter);
            
            fallback.append(String.format("📊 **Vận hành**: %s có **%d chuyến** (", 
                    periodLabel, operationalSummary.getTotalTrips()));
            
            List<String> tripStats = new ArrayList<>();
            if (operationalSummary.getActiveTrips() > 0) tripStats.add(operationalSummary.getActiveTrips() + " đang hoạt động");
            if (operationalSummary.getCompletedTrips() > 0) tripStats.add(operationalSummary.getCompletedTrips() + " hoàn thành");
            if (operationalSummary.getDelayedTrips() > 0) tripStats.add("⚠️ " + operationalSummary.getDelayedTrips() + " trễ hẹn");
            
            fallback.append(String.join(", ", tripStats)).append("). ");
            
            long totalOpenIssues = issueSummary.getOpenIssues() + issueSummary.getInProgressIssues();
            if (totalOpenIssues > 0) {
                fallback.append(String.format("🚨 **Sự cố**: **%d** cần xử lý (%d mở, %d đang xử lý). ", 
                        totalOpenIssues, issueSummary.getOpenIssues(), issueSummary.getInProgressIssues()));
            } else {
                fallback.append("✅ **Sự cố**: Không có sự cố cần xử lý. ");
            }
            
            fallback.append(String.format("🚛 **Đội xe**: **%d/%d** xe sẵn sàng. ", 
                    fleetStatus.getAvailableVehicles(), fleetStatus.getTotalVehicles()));
            
            return fallback.toString();
        }
    }

    @Override
    public CustomerDashboardResponse getCustomerDashboard(UUID customerId, DashboardFilterRequest filter) {
        LocalDateTime startDate = filter.getStartDate();
        LocalDateTime endDate = filter.getEndDate();
        
        log.info("[Customer Dashboard] Getting data for customer {} from {} to {}", customerId, startDate, endDate);

        // Get customer's orders, then apply global time filter for ALL sections
        List<OrderEntity> customerOrders = orderEntityService.findAll().stream()
                .filter(o -> o.getSender() != null && o.getSender().getId().equals(customerId))
                .collect(Collectors.toList());

        List<OrderEntity> filteredOrders = filterByDateRange(customerOrders, startDate, endDate);

        // All downstream calculations (order summary, distributions, financials, actions, activities)
        // will be based on filteredOrders so that everything respects the dashboard filter.

        // Order details for filtered period
        List<OrderDetailEntity> allOrderDetails = new ArrayList<>();
        for (OrderEntity order : filteredOrders) {
            allOrderDetails.addAll(order.getOrderDetailEntities());
        }

        // Contracts belonging to filtered orders
        List<ContractEntity> customerContracts = new ArrayList<>();
        for (OrderEntity order : filteredOrders) {
            contractEntityService.getContractByOrderId(order.getId()).ifPresent(customerContracts::add);
        }
        
        // Get transactions for the filtered contracts
        List<TransactionEntity> customerTransactions = new ArrayList<>();
        for (ContractEntity contract : customerContracts) {
            customerTransactions.addAll(transactionEntityService.findByContractId(contract.getId()));
        }
        
        // Get issues for customer's orders
        List<IssueEntity> customerIssues = new ArrayList<>();
        for (OrderDetailEntity od : allOrderDetails) {
            if (od.getIssueEntity() != null) {
                customerIssues.add(od.getIssueEntity());
            }
        }

        // Order summary (filtered orders & details)
        CustomerDashboardResponse.OrderSummary orderSummary = buildCustomerOrderSummary(filteredOrders, allOrderDetails);
        
        // Order status distribution (for orders)
        Map<String, Long> orderStatusDistribution = allOrderDetails.stream()
                .filter(od -> od.getStatus() != null)
                .collect(Collectors.groupingBy(OrderDetailEntity::getStatus, Collectors.counting()));
        
        // Order detail status distribution (for packages)
        Map<String, Long> orderDetailStatusDistribution = allOrderDetails.stream()
                .filter(od -> od.getStatus() != null)
                .collect(Collectors.groupingBy(OrderDetailEntity::getStatus, Collectors.counting()));
        
        // Delivery performance (within filtered period)
        CustomerDashboardResponse.DeliveryPerformance deliveryPerformance = buildCustomerDeliveryPerformance(allOrderDetails, customerIssues);

        // Financial summary (everything already filtered by period)
        CustomerDashboardResponse.FinancialSummary financialSummary = buildCustomerFinancialSummary(
                customerContracts, customerTransactions, filter);

        // Active orders (only filtered orders)
        List<CustomerDashboardResponse.ActiveOrderItem> activeOrders = buildActiveOrders(filteredOrders);
        
        // Actions summary
        CustomerDashboardResponse.ActionsSummary actionsSummary = buildActionsSummary(customerContracts, customerIssues);
        
        // Recent activity (filtered orders & transactions)
        List<CustomerDashboardResponse.ActivityItem> recentActivity = buildRecentActivity(filteredOrders, customerTransactions);
        
        // Recent issues (exclude OFFROUTE and TRAFFIC_PENALTY, within filtered details)
        List<CustomerDashboardResponse.RecentIssue> recentIssues = buildRecentIssues(customerIssues);
        
        // Top recipients (from filtered order details)
        List<CustomerDashboardResponse.TopRecipient> topRecipients = buildTopRecipients(allOrderDetails);
        
        // Package status trend for visualization (filtered details)
        List<CustomerDashboardResponse.PackageStatusTrend> packageStatusTrend = buildPackageStatusTrend(allOrderDetails, filter);

        return CustomerDashboardResponse.builder()
                .orderSummary(orderSummary)
                .orderStatusDistribution(orderStatusDistribution)
                .orderDetailStatusDistribution(orderDetailStatusDistribution)
                .deliveryPerformance(deliveryPerformance)
                .financialSummary(financialSummary)
                .activeOrders(activeOrders)
                .actionsSummary(actionsSummary)
                .recentActivity(recentActivity)
                .recentIssues(recentIssues)
                .topRecipients(topRecipients)
                .packageStatusTrend(packageStatusTrend)
                .build();
    }

    @Override
    public String getCustomerAiSummary(UUID customerId, DashboardFilterRequest filter) {
        LocalDateTime startDate = filter.getStartDate();
        LocalDateTime endDate = filter.getEndDate();
        
        log.info("[Customer AI Summary] Generating summary for customer {} from {} to {}", customerId, startDate, endDate);

        // Get customer's orders, then apply global time filter
        List<OrderEntity> customerOrders = orderEntityService.findAll().stream()
                .filter(o -> o.getSender() != null && o.getSender().getId().equals(customerId))
                .collect(Collectors.toList());

        List<OrderEntity> filteredOrders = filterByDateRange(customerOrders, startDate, endDate);

        // Order details for filtered period
        List<OrderDetailEntity> allOrderDetails = new ArrayList<>();
        for (OrderEntity order : filteredOrders) {
            allOrderDetails.addAll(order.getOrderDetailEntities());
        }

        // Contracts belonging to filtered orders
        List<ContractEntity> customerContracts = new ArrayList<>();
        for (OrderEntity order : filteredOrders) {
            contractEntityService.getContractByOrderId(order.getId()).ifPresent(customerContracts::add);
        }
        
        // Get transactions for the filtered contracts
        List<TransactionEntity> customerTransactions = new ArrayList<>();
        for (ContractEntity contract : customerContracts) {
            customerTransactions.addAll(transactionEntityService.findByContractId(contract.getId()));
        }
        
        // Get issues for customer's orders
        List<IssueEntity> customerIssues = new ArrayList<>();
        for (OrderDetailEntity od : allOrderDetails) {
            if (od.getIssueEntity() != null) {
                customerIssues.add(od.getIssueEntity());
            }
        }

        // Build summary data
        CustomerDashboardResponse.OrderSummary orderSummary = buildCustomerOrderSummary(filteredOrders, allOrderDetails);
        CustomerDashboardResponse.DeliveryPerformance deliveryPerformance = buildCustomerDeliveryPerformance(allOrderDetails, customerIssues);
        CustomerDashboardResponse.FinancialSummary financialSummary = buildCustomerFinancialSummary(customerContracts, customerTransactions, filter);
        CustomerDashboardResponse.ActionsSummary actionsSummary = buildActionsSummary(customerContracts, customerIssues);

        // Generate AI Summary with fallback
        try {
            return generateCustomerAiSummary(orderSummary, deliveryPerformance, financialSummary, actionsSummary, filter);
        } catch (Exception e) {
            log.error("[Customer AI Summary] Error generating AI summary, using fallback", e);
            
            // Fallback summary (simple, no AI)
            StringBuilder fallback = new StringBuilder();
            String periodLabel = getPeriodLabel(filter);
            
            fallback.append(String.format("Trong %s, bạn có **%d đơn hàng** với **%d kiện hàng**. ",
                    periodLabel, orderSummary.getTotalOrders(), orderSummary.getTotalOrderDetails()));

            if (orderSummary.getDeliveredPackages() > 0) {
                fallback.append(String.format("**%d kiện** đã giao thành công (tỷ lệ: **%.1f%%**). ",
                        orderSummary.getDeliveredPackages(), orderSummary.getSuccessRate()));
            }

            if (orderSummary.getInTransitPackages() > 0) {
                fallback.append(String.format("**%d kiện** đang vận chuyển. ", orderSummary.getInTransitPackages()));
            }

            if (actionsSummary.getContractsToSign() > 0) {
                fallback.append(String.format("Bạn có **%d hợp đồng** cần ký. ", actionsSummary.getContractsToSign()));
            }

            if (actionsSummary.getPaymentsNeeded() > 0) {
                fallback.append(String.format("Có **%d thanh toán** đang chờ. ", actionsSummary.getPaymentsNeeded()));
            }

            if (deliveryPerformance.getIssueCount() > 0) {
                fallback.append(String.format("Có **%d sự cố** liên quan đến đơn hàng của bạn. ", deliveryPerformance.getIssueCount()));
            }
            
            return fallback.toString();
        }
    }

    @Override
    public DriverDashboardResponse getDriverDashboard(UUID driverId, DashboardFilterRequest filter) {
        LocalDateTime startDate = filter.getStartDate();
        LocalDateTime endDate = filter.getEndDate();
        
        log.info("[Driver Dashboard] Getting simplified data for driver {} from {} to {}", driverId, startDate, endDate);

        // Get driver's assignments within the filter period
        List<VehicleAssignmentEntity> allDriverAssignments = vehicleAssignmentEntityService.findAssignmentsForDriverSince(driverId, startDate);
        List<VehicleAssignmentEntity> periodAssignments = allDriverAssignments.stream()
                .filter(a -> a.getCreatedAt() != null && 
                        !a.getCreatedAt().isBefore(startDate) && 
                        !a.getCreatedAt().isAfter(endDate))
                .collect(Collectors.toList());
        
        // Calculate key metrics
        int completedTripsCount = (int) periodAssignments.stream()
                .filter(a -> "COMPLETED".equals(a.getStatus()))
                .count();
        
        int incidentsCount = getIncidentsCountForDriver(driverId, startDate, endDate);
        int trafficViolationsCount = getTrafficViolationsCountForDriver(driverId, startDate, endDate);
        
        // Trip trend data for line graph (completed trips only)
        List<DriverDashboardResponse.TripTrendPoint> tripTrend = buildTripTrendData(periodAssignments, filter);
        
        // Recent orders (latest 5 assignments)
        List<DriverDashboardResponse.RecentOrder> recentOrders = buildRecentOrders(allDriverAssignments);

        return DriverDashboardResponse.builder()
                .completedTripsCount(completedTripsCount)
                .incidentsCount(incidentsCount)
                .trafficViolationsCount(trafficViolationsCount)
                .tripTrend(tripTrend)
                .recentOrders(recentOrders)
                .build();
    }

    @Override
    public String getDriverAiSummary(UUID driverId, DashboardFilterRequest filter) {
        LocalDateTime startDate = filter.getStartDate();
        LocalDateTime endDate = filter.getEndDate();
        
        log.info("[Driver AI Summary] Generating summary for driver {} from {} to {}", driverId, startDate, endDate);

        // Get driver's assignments within the filter period
        List<VehicleAssignmentEntity> allDriverAssignments = vehicleAssignmentEntityService.findAssignmentsForDriverSince(driverId, startDate);
        List<VehicleAssignmentEntity> periodAssignments = allDriverAssignments.stream()
                .filter(a -> a.getCreatedAt() != null && 
                        !a.getCreatedAt().isBefore(startDate) && 
                        !a.getCreatedAt().isAfter(endDate))
                .collect(Collectors.toList());
        
        // Calculate key metrics
        int completedTripsCount = (int) periodAssignments.stream()
                .filter(a -> "COMPLETED".equals(a.getStatus()))
                .count();
        
        int incidentsCount = getIncidentsCountForDriver(driverId, startDate, endDate);
        int trafficViolationsCount = getTrafficViolationsCountForDriver(driverId, startDate, endDate);
        
        String periodLabel = getPeriodLabel(filter);
        
        // Fallback summary
        StringBuilder fallback = new StringBuilder();
        if (completedTripsCount > 0) {
            fallback.append(String.format("Trong %s bạn đã hoàn thành **%d chuyến**. ", 
                    periodLabel, completedTripsCount));
        } else {
            fallback.append(String.format("Trong %s bạn chưa có chuyến nào được hoàn thành. ", periodLabel));
        }
        
        if (incidentsCount > 0) {
            fallback.append(String.format("Có **%d sự cố** cần chú ý. ", incidentsCount));
        }
        
        if (trafficViolationsCount > 0) {
            fallback.append(String.format("Có **%d vi phạm giao thông**. ", trafficViolationsCount));
        }
        
        // Gọi Gemini để tạo tóm tắt giàu ngữ cảnh hơn
        try {
            String systemPrompt = "Bạn là trợ lý phân tích cho tài xế của hệ thống quản lý vận tải. " +
                    "Hãy tóm tắt ngắn gọn (3-5 câu) tình hình làm việc trong " + periodLabel +
                    " bằng tiếng Việt, giọng thân thiện, tập trung vào hiệu suất cá nhân và các điểm cần cải thiện. " +
                    "Phải phân tích TẤT CẢ các số liệu sau: tổng chuyến hoàn thành, sự cố, vi phạm giao thông. " +
                    "Nếu hiệu suất tốt, hãy khen ngợi. Nếu có vấn đề, hãy nhẹ nhàng gợi ý. " +
                    "QUAN TRỌNG: Sử dụng định dạng **in đậm** cho tất cả các số liệu quan trọng.";

            StringBuilder userContext = new StringBuilder();
            userContext.append("=== PHÂN TÍCH DỮ LIỆU DASHBOARD TÀI XẾ ===\n");
            userContext.append("=== HIỆU SUẤT CHUYẾN ===\n");
            userContext.append(String.format("• Chuyến đã hoàn thành: %d\n", completedTripsCount));
            userContext.append("=== SỰ CỐ VÀ VI PHẠM ===\n");
            userContext.append(String.format("• Sự cố đã gặp: %d\n", incidentsCount));
            userContext.append(String.format("• Vi phạm giao thông: %d\n", trafficViolationsCount));
            
            // Log the exact context being sent to AI for debugging
            log.info("[Driver AI Summary] Sending to AI:\n{}", userContext.toString());

            List<ChatMessage> messages = List.of(
                    new ChatMessage("user", userContext.toString())
            );

            String geminiSummary = geminiService.generateResponse(systemPrompt, messages);
            if (geminiSummary != null && !geminiSummary.isBlank()) {
                log.info("[Driver AI Summary] AI Response: {}", geminiSummary);
                return geminiSummary;
            }
        } catch (Exception e) {
            log.error("[Driver AI Summary] Gemini summary failed, using fallback summary", e);
        }

        return fallback.toString();
    }

    // ==================== Helper Methods ====================

    private List<OrderEntity> filterByDateRange(List<OrderEntity> orders, LocalDateTime start, LocalDateTime end) {
        return orders.stream()
                .filter(o -> o.getCreatedAt() != null && 
                        !o.getCreatedAt().isBefore(start) && 
                        !o.getCreatedAt().isAfter(end))
                .collect(Collectors.toList());
    }

    private AdminDashboardResponse.KpiSummary buildAdminKpiSummary(
            List<OrderEntity> orders, 
            List<OrderDetailEntity> orderDetails,
            List<TransactionEntity> transactions,
            List<IssueEntity> issues,
            List<RefundEntity> refunds) {
        
        BigDecimal totalRevenue = transactions.stream()
                .filter(t -> "PAID".equals(t.getStatus()))
                .map(TransactionEntity::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        long completedDeliveries = orderDetails.stream()
                .filter(od -> "SUCCESSFUL".equals(od.getStatus()) || "DELIVERED".equals(od.getStatus()))
                .count();
        
        long totalDeliveries = orderDetails.stream()
                .filter(od -> "SUCCESSFUL".equals(od.getStatus()) || "DELIVERED".equals(od.getStatus()))
                .count();
        
        double onTimePercentage = totalDeliveries > 0 
                ? (double) completedDeliveries / totalDeliveries * 100 
                : 0.0;
        
        double issueRate = orderDetails.size() > 0 
                ? (double) issues.size() / orderDetails.size() * 100 
                : 0.0;
        
        BigDecimal refundAmount = refunds.stream()
                .map(RefundEntity::getRefundAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return AdminDashboardResponse.KpiSummary.builder()
                .totalOrders((long) orders.size())
                .totalOrderDetails((long) orderDetails.size())
                .totalRevenue(totalRevenue)
                .onTimePercentage(Math.round(onTimePercentage * 100.0) / 100.0)
                .issueRate(Math.round(issueRate * 100.0) / 100.0)
                .newCustomers(0L) // Would need customer creation date tracking
                .refundAmount(refundAmount)
                .orderGrowth(0.0) // Would need previous period comparison
                .revenueGrowth(0.0)
                .onTimeGrowthChange(0.0)
                .build();
    }

    private List<AdminDashboardResponse.TrendDataPoint> buildOrderTrend(
            List<OrderEntity> orders, DashboardFilterRequest filter) {

        // Generate full label set for selected range
        List<String> allDateLabels = generateAllDateLabels(filter);

        // Group orders by normalized date key
        Map<String, Long> ordersByDate = orders.stream()
                .filter(o -> o.getCreatedAt() != null)
                .collect(Collectors.groupingBy(
                        o -> getDateKey(o.getCreatedAt(), filter),
                        Collectors.counting()));

        // Build trend with all dates in range (count 0 if no orders)
        return allDateLabels.stream()
                .map(label -> AdminDashboardResponse.TrendDataPoint.builder()
                        .label(label)
                        .count(ordersByDate.getOrDefault(label, 0L))
                        .amount(BigDecimal.ZERO)
                        .build())
                .collect(Collectors.toList());
    }
    
    private List<AdminDashboardResponse.TrendDataPoint> buildRevenueTrend(
            List<TransactionEntity> transactions, DashboardFilterRequest filter) {

        // Generate full label set for selected range
        List<String> allDateLabels = generateAllDateLabels(filter);

        // Group revenue by normalized date key
        Map<String, BigDecimal> revenueByDate = transactions.stream()
                .filter(t -> t.getPaymentDate() != null && "PAID".equals(t.getStatus()))
                .collect(Collectors.groupingBy(
                        t -> getDateKey(t.getPaymentDate(), filter),
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                t -> t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO,
                                BigDecimal::add
                        )
                ));

        // Build trend with all dates in range (0 amount if no revenue)
        return allDateLabels.stream()
                .map(label -> AdminDashboardResponse.TrendDataPoint.builder()
                        .label(label)
                        .count(0)
                        .amount(revenueByDate.getOrDefault(label, BigDecimal.ZERO))
                        .build())
                .collect(Collectors.toList());
    }

    private AdminDashboardResponse.DeliveryPerformance buildDeliveryPerformance(List<OrderDetailEntity> orderDetails) {
        // Since endTime and estimatedEndTime fields have been removed,
        // we'll use status to determine on-time vs late deliveries
        long onTime = orderDetails.stream()
                .filter(od -> "DELIVERED".equals(od.getStatus()) || "SUCCESSFUL".equals(od.getStatus()))
                .count();
        
        // For now, we'll consider all deliveries on time since we can't determine lateness without endTime
        long late = 0;
        
        long total = onTime + late;
        double onTimePercentage = total > 0 ? (double) onTime / total * 100 : 0;
        double latePercentage = total > 0 ? (double) late / total * 100 : 0;

        return AdminDashboardResponse.DeliveryPerformance.builder()
                .onTimeCount(onTime)
                .lateCount(late)
                .onTimePercentage(Math.round(onTimePercentage * 100.0) / 100.0)
                .latePercentage(Math.round(latePercentage * 100.0) / 100.0)
                .trend(new ArrayList<>())
                .build();
    }

    private AdminDashboardResponse.IssueRefundSummary buildIssueRefundSummary(List<IssueEntity> issues, List<RefundEntity> refunds) {
        long openIssues = issues.stream().filter(i -> "OPEN".equals(i.getStatus()) || "REPORTED".equals(i.getStatus())).count();
        long resolvedIssues = issues.stream().filter(i -> "RESOLVED".equals(i.getStatus()) || "CLOSED".equals(i.getStatus())).count();
        
        Map<String, Long> issuesByType = issues.stream()
                .filter(i -> i.getIssueTypeEntity() != null)
                .collect(Collectors.groupingBy(
                        i -> i.getIssueTypeEntity().getIssueTypeName(),
                        Collectors.counting()));
        
        long pendingRefunds = refunds.stream().filter(r -> r.getRefundDate() == null).count();
        long completedRefunds = refunds.stream().filter(r -> r.getRefundDate() != null).count();
        
        BigDecimal totalRefundAmount = refunds.stream()
                .map(RefundEntity::getRefundAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return AdminDashboardResponse.IssueRefundSummary.builder()
                .totalIssues((long) issues.size())
                .openIssues(openIssues)
                .resolvedIssues(resolvedIssues)
                .pendingRefunds(pendingRefunds)
                .completedRefunds(completedRefunds)
                .totalRefundAmount(totalRefundAmount)
                .issuesByType(issuesByType)
                .build();
    }

    private List<AdminDashboardResponse.TopPerformer> buildTopCustomers() {
        try {
            List<Object[]> results = customerEntityService.getTopCustomersByRevenue(5);
            List<AdminDashboardResponse.TopPerformer> topCustomers = new ArrayList<>();
            int rank = 1;
            for (Object[] row : results) {
                topCustomers.add(AdminDashboardResponse.TopPerformer.builder()
                        .id(row[0].toString())
                        .name((String) row[1])
                        .companyName((String) row[2])
                        .revenue((BigDecimal) row[3])
                        .rank(rank++)
                        .build());
            }
            return topCustomers;
        } catch (Exception e) {
            log.warn("Failed to get top customers: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<AdminDashboardResponse.TopPerformer> buildTopDrivers() {
        try {
            List<Object[]> results = orderEntityService.topDriverByMonthAndYear(null, null, 5);
            List<AdminDashboardResponse.TopPerformer> topDrivers = new ArrayList<>();
            for (Object[] row : results) {
                topDrivers.add(AdminDashboardResponse.TopPerformer.builder()
                        .id(row[0].toString())
                        .name((String) row[1])
                        .orderCount(((Number) row[2]).longValue())
                        .rank(((Number) row[3]).intValue())
                        .build());
            }
            return topDrivers;
        } catch (Exception e) {
            log.warn("Failed to get top drivers: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private AdminDashboardResponse.FleetHealthSummary buildFleetHealth(List<VehicleEntity> vehicles, List<VehicleServiceRecordEntity> maintenances) {
        long activeVehicles = vehicles.stream().filter(v -> "ACTIVE".equals(v.getStatus())).count();
        long inTransitVehicles = vehicles.stream().filter(v -> "IN_TRANSIT".equals(v.getStatus())).count();
        long inMaintenance = vehicles.stream().filter(v -> "MAINTENANCE".equals(v.getStatus())).count();
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextWeek = now.plusWeeks(1);
        
        List<AdminDashboardResponse.MaintenanceAlert> alerts = maintenances.stream()
                .filter(m -> m.getPlannedDate() != null && m.getPlannedDate().toLocalDate().isBefore(nextWeek.toLocalDate()))
                .map(m -> AdminDashboardResponse.MaintenanceAlert.builder()
                        .vehicleId(m.getVehicleEntity() != null ? m.getVehicleEntity().getId().toString() : "")
                        .licensePlate(m.getVehicleEntity() != null ? m.getVehicleEntity().getLicensePlateNumber() : "")
                        .maintenanceType(m.getServiceType() != null ? m.getServiceType() : "")
                        .dueDate(m.getPlannedDate().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE))
                        .isOverdue(m.getPlannedDate().toLocalDate().isBefore(now.toLocalDate()))
                        .build())
                .limit(5)
                .collect(Collectors.toList());

        return AdminDashboardResponse.FleetHealthSummary.builder()
                .totalVehicles((long) vehicles.size())
                .activeVehicles(activeVehicles)
                .inUseVehicles(inTransitVehicles)
                .inMaintenanceVehicles(inMaintenance)
                .pendingMaintenanceVehicles((long) alerts.size())
                .overdueMaintenanceVehicles(alerts.stream().filter(AdminDashboardResponse.MaintenanceAlert::isOverdue).count())
                .averageFuelConsumption(BigDecimal.ZERO)
                .upcomingMaintenances(alerts)
                .build();
    }

    private AdminDashboardResponse.DeviceStatistics buildDeviceStatistics() {
        try {
            var allDevices = deviceEntityService.findAll();
            long totalDevices = allDevices.size();
            long activeDevices = allDevices.stream()
                    .filter(d -> "ACTIVE".equals(d.getStatus()))
                    .count();
            long inactiveDevices = allDevices.stream()
                    .filter(d -> "INACTIVE".equals(d.getStatus()))
                    .count();
            long assignedDevices = allDevices.stream()
                    .filter(d -> d.getVehicleEntity() != null)
                    .count();
            
            return AdminDashboardResponse.DeviceStatistics.builder()
                    .totalDevices(totalDevices)
                    .activeDevices(activeDevices)
                    .inactiveDevices(inactiveDevices)
                    .assignedDevices(assignedDevices)
                    .deltaPercent(0.0)
                    .build();
        } catch (Exception e) {
            log.warn("Failed to build device statistics: {}", e.getMessage());
            return AdminDashboardResponse.DeviceStatistics.builder()
                    .totalDevices(0)
                    .activeDevices(0)
                    .inactiveDevices(0)
                    .assignedDevices(0)
                    .deltaPercent(0.0)
                    .build();
        }
    }

    private AdminDashboardResponse.FuelConsumptionStatistics buildFuelConsumptionStatistics(
            LocalDateTime startDate, LocalDateTime endDate, DashboardFilterRequest filter) {
        try {
            var allFuelConsumptions = vehicleFuelConsumptionEntityService.findAll();
            var filteredFuelConsumptions = allFuelConsumptions.stream()
                    .filter(fc -> fc.getDateRecorded() != null &&
                            !fc.getDateRecorded().isBefore(startDate) &&
                            !fc.getDateRecorded().isAfter(endDate))
                    .collect(Collectors.toList());
            
            BigDecimal totalFuelConsumed = filteredFuelConsumptions.stream()
                    .map(fc -> fc.getFuelVolume() != null ? fc.getFuelVolume() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal averageFuelConsumption = filteredFuelConsumptions.isEmpty() 
                    ? BigDecimal.ZERO 
                    : totalFuelConsumed.divide(BigDecimal.valueOf(filteredFuelConsumptions.size()), 2, RoundingMode.HALF_UP);
            
            // Build trend data
            List<AdminDashboardResponse.TrendDataPoint> fuelTrend = buildFuelConsumptionTrend(filteredFuelConsumptions, filter);
            
            return AdminDashboardResponse.FuelConsumptionStatistics.builder()
                    .totalFuelConsumed(totalFuelConsumed)
                    .averageFuelConsumption(averageFuelConsumption)
                    .deltaPercent(0.0)
                    .fuelConsumptionTrend(fuelTrend)
                    .build();
        } catch (Exception e) {
            log.warn("Failed to build fuel consumption statistics: {}", e.getMessage());
            return AdminDashboardResponse.FuelConsumptionStatistics.builder()
                    .totalFuelConsumed(BigDecimal.ZERO)
                    .averageFuelConsumption(BigDecimal.ZERO)
                    .deltaPercent(0.0)
                    .fuelConsumptionTrend(new ArrayList<>())
                    .build();
        }
    }

    private List<AdminDashboardResponse.TrendDataPoint> buildFuelConsumptionTrend(
            List<capstone_project.entity.order.order.VehicleFuelConsumptionEntity> fuelConsumptions,
            DashboardFilterRequest filter) {
        Map<String, BigDecimal> groupedData = new LinkedHashMap<>();
        
        for (var fc : fuelConsumptions) {
            if (fc.getDateRecorded() == null) continue;
            String label = formatDateLabel(fc.getDateRecorded(), filter);
            BigDecimal fuelVolume = fc.getFuelVolume() != null ? fc.getFuelVolume() : BigDecimal.ZERO;
            groupedData.merge(label, fuelVolume, BigDecimal::add);
        }
        
        return groupedData.entrySet().stream()
                .map(entry -> AdminDashboardResponse.TrendDataPoint.builder()
                        .label(entry.getKey())
                        .count(0)
                        .amount(entry.getValue())
                        .build())
                .collect(Collectors.toList());
    }

    private AdminDashboardResponse.PenaltiesStatistics buildPenaltiesStatistics(
            LocalDateTime startDate, LocalDateTime endDate, DashboardFilterRequest filter) {
        try {
            var allPenalties = penaltyHistoryRepository.findAll();
            var filteredPenalties = allPenalties.stream()
                    .filter(p -> p.getPenaltyDate() != null &&
                            !p.getPenaltyDate().isBefore(startDate.toLocalDate()) &&
                            !p.getPenaltyDate().isAfter(endDate.toLocalDate()))
                    .collect(Collectors.toList());
            
            long totalPenalties = filteredPenalties.size();
            // PenaltyHistoryEntity doesn't have status field, count all as unresolved for now
            long unresolvedPenalties = filteredPenalties.size();
            
            // Build trend data
            List<AdminDashboardResponse.TrendDataPoint> penaltiesTrend = buildPenaltiesTrend(filteredPenalties, filter);
            
            return AdminDashboardResponse.PenaltiesStatistics.builder()
                    .totalPenalties(totalPenalties)
                    .unresolvedPenalties(unresolvedPenalties)
                    .deltaPercent(0.0)
                    .penaltiesTrend(penaltiesTrend)
                    .build();
        } catch (Exception e) {
            log.warn("Failed to build penalties statistics: {}", e.getMessage());
            return AdminDashboardResponse.PenaltiesStatistics.builder()
                    .totalPenalties(0)
                    .unresolvedPenalties(0)
                    .deltaPercent(0.0)
                    .penaltiesTrend(new ArrayList<>())
                    .build();
        }
    }

    private List<AdminDashboardResponse.TrendDataPoint> buildPenaltiesTrend(
            List<PenaltyHistoryEntity> penalties, DashboardFilterRequest filter) {
        Map<String, Long> groupedData = new LinkedHashMap<>();
        
        for (var p : penalties) {
            if (p.getPenaltyDate() == null) continue;
            LocalDateTime dateTime = p.getPenaltyDate().atStartOfDay();
            String label = formatDateLabel(dateTime, filter);
            groupedData.merge(label, 1L, Long::sum);
        }
        
        return groupedData.entrySet().stream()
                .map(entry -> AdminDashboardResponse.TrendDataPoint.builder()
                        .label(entry.getKey())
                        .count(entry.getValue())
                        .amount(BigDecimal.ZERO)
                        .build())
                .collect(Collectors.toList());
    }

    private List<AdminDashboardResponse.VehicleInspectionAlert> buildVehicleInspectionAlerts(
            List<VehicleEntity> vehicles, List<VehicleServiceRecordEntity> maintenances) {
        List<AdminDashboardResponse.VehicleInspectionAlert> alerts = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threeMonthsFromNow = now.plusMonths(3);
        
        LocalDate today = now.toLocalDate();
        LocalDate threeMonthsFromNowDate = threeMonthsFromNow.toLocalDate();
        
        for (VehicleEntity vehicle : vehicles) {
            // Check inspection expiry date (LocalDate)
            if (vehicle.getInspectionExpiryDate() != null) {
                LocalDate inspectionExpiry = vehicle.getInspectionExpiryDate();
                if (inspectionExpiry.isBefore(threeMonthsFromNowDate)) {
                    int daysUntilDue = (int) java.time.temporal.ChronoUnit.DAYS.between(today, inspectionExpiry);
                    alerts.add(AdminDashboardResponse.VehicleInspectionAlert.builder()
                            .vehicleId(vehicle.getId().toString())
                            .licensePlate(vehicle.getLicensePlateNumber())
                            .alertType("INSPECTION")
                            .dueDate(inspectionExpiry.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE))
                            .daysUntilDue(daysUntilDue)
                            .isOverdue(inspectionExpiry.isBefore(today))
                            .description("Đăng kiểm xe")
                            .build());
                }
            }
            
            // Check next maintenance date (LocalDate)
            if (vehicle.getNextMaintenanceDate() != null) {
                LocalDate nextMaintenance = vehicle.getNextMaintenanceDate();
                if (nextMaintenance.isBefore(threeMonthsFromNowDate)) {
                    int daysUntilDue = (int) java.time.temporal.ChronoUnit.DAYS.between(today, nextMaintenance);
                    alerts.add(AdminDashboardResponse.VehicleInspectionAlert.builder()
                            .vehicleId(vehicle.getId().toString())
                            .licensePlate(vehicle.getLicensePlateNumber())
                            .alertType("MAINTENANCE")
                            .dueDate(nextMaintenance.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE))
                            .daysUntilDue(daysUntilDue)
                            .isOverdue(nextMaintenance.isBefore(today))
                            .description("Bảo dưỡng định kỳ")
                            .build());
                }
            }
        }
        
        // Sort by days until due (overdue first, then closest to due)
        alerts.sort(Comparator.comparingInt(AdminDashboardResponse.VehicleInspectionAlert::getDaysUntilDue));
        
        return alerts.stream().limit(10).collect(Collectors.toList());
    }

    private String formatDateLabel(LocalDateTime dateTime, DashboardFilterRequest filter) {
        if (filter.getRange() == null) {
            return dateTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }
        
        String rangeStr = filter.getRange().name();
        return switch (rangeStr) {
            case "WEEK" -> dateTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            case "MONTH" -> "Tuần " + ((dateTime.getDayOfMonth() - 1) / 7 + 1);
            case "YEAR" -> dateTime.format(java.time.format.DateTimeFormatter.ofPattern("MM/yyyy"));
            default -> dateTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        };
    }

    private String generateAdminAiSummary(
            AdminDashboardResponse.KpiSummary kpi,
            AdminDashboardResponse.DeliveryPerformance delivery,
            AdminDashboardResponse.IssueRefundSummary issues,
            AdminDashboardResponse.FleetHealthSummary fleet,
            DashboardFilterRequest filter) {
        
        String periodLabel = getPeriodLabel(filter);
        
        // Fallback summary
        StringBuilder fallback = new StringBuilder();
        fallback.append(String.format("Trong %s, hệ thống có **%d đơn hàng** với **%d kiện hàng**, ", 
                periodLabel, kpi.getTotalOrders(), kpi.getTotalOrderDetails()));
        fallback.append(String.format("doanh thu đạt **%s VNĐ**. ", formatCurrency(kpi.getTotalRevenue())));
        fallback.append(String.format("Tỷ lệ giao hàng đúng hẹn: **%.1f%%** (%d đúng hẹn, %d trễ). ", 
                delivery.getOnTimePercentage(), delivery.getOnTimeCount(), delivery.getLateCount()));
        
        if (issues.getOpenIssues() > 0) {
            fallback.append(String.format("Có **%d sự cố** đang mở cần xử lý. ", issues.getOpenIssues()));
        }
        
        if (fleet.getOverdueMaintenanceVehicles() > 0) {
            fallback.append(String.format("**%d xe** đã quá hạn bảo dưỡng. ", fleet.getOverdueMaintenanceVehicles()));
        }
        
        // Gọi Gemini để tạo tóm tắt giàu ngữ cảnh hơn
        try {
            String systemPrompt = "Bạn là trợ lý phân tích cho quản trị viên của hệ thống quản lý vận tải. " +
                    "Hãy tóm tắt ngắn gọn (3-5 câu) tình hình kinh doanh trong " + periodLabel +
                    " bằng tiếng Việt, giọng chuyên nghiệp, tập trung vào hiệu suất tổng quan và các điểm cần chú ý. " +
                    "Phải phân tích TẤT CẢ các số liệu sau: doanh thu, đơn hàng, hiệu suất giao hàng (đúng hẹn/trễ), sự cố (mở/đã giải quyết), hoàn tiền, đội xe (khả dụng/bảo dưỡng), " +
                    "VÀ QUAN TRỌNG NHẤT: đánh giá xu hướng và đưa ra khuyến nghị cải thiện. " +
                    "Nếu có vấn đề nghiêm trọng (nhiều sự cố, xe quá hạn, tỷ lệ trễ cao), hãy nhấn mạnh và đề xuất hành động ngay. " +
                    "QUAN TRỌNG: Sử dụng định dạng **in đậm** cho tất cả các số liệu quan trọng, tỷ lệ phần trăm, và những điểm cần chú ý đặc biệt.";

            StringBuilder userContext = new StringBuilder();
            userContext.append("=== PHÂN TÍCH DỮ LIỆU DASHBOARD QUẢN TRỊ ===\n");
            userContext.append("=== KINH DOANH ===\n");
            userContext.append(String.format("• Tổng đơn hàng: %d\n", kpi.getTotalOrders()));
            userContext.append(String.format("• Tổng kiện hàng: %d\n", kpi.getTotalOrderDetails()));
            userContext.append(String.format("• Doanh thu: %s VNĐ\n", kpi.getTotalRevenue().toString()));
                                    userContext.append("=== HIỆU SUẤT GIAO HÀNG ===\n");
            userContext.append(String.format("• Tỷ lệ đúng hẹn: %.1f%%\n", delivery.getOnTimePercentage()));
            userContext.append(String.format("• Giao đúng hẹn: %d\n", delivery.getOnTimeCount()));
            userContext.append(String.format("• Giao trễ: %d\n", delivery.getLateCount()));
                        userContext.append("=== SỰ CỐ VÀ HOÀN TIỀN ===\n");
            userContext.append(String.format("• Sự cố đang mở: %d\n", issues.getOpenIssues()));
            userContext.append(String.format("• Sự cố đã giải quyết: %d\n", issues.getResolvedIssues()));
            userContext.append(String.format("• Tổng sự cố: %d\n", issues.getTotalIssues()));
            userContext.append(String.format("• Hoàn tiền đang chờ: %d\n", issues.getPendingRefunds()));
            userContext.append(String.format("• Tổng tiền hoàn tiền: %s VNĐ\n", issues.getTotalRefundAmount().toString()));
            userContext.append("=== ĐỘI XE ===\n");
            userContext.append(String.format("• Tổng xe: %d\n", fleet.getTotalVehicles()));
                        userContext.append(String.format("• Xe đang hoạt động: %d\n", fleet.getActiveVehicles()));
            userContext.append(String.format("• Xe đang bảo dưỡng: %d\n", fleet.getInMaintenanceVehicles()));
            userContext.append(String.format("• Xe quá hạn bảo dưỡng: %d\n", fleet.getOverdueMaintenanceVehicles()));
            if (fleet.getTotalVehicles() > 0) {
                double utilizationRate = (double) fleet.getActiveVehicles() / fleet.getTotalVehicles() * 100;
                userContext.append(String.format("• Tỷ lệ sử dụng đội xe: %.1f%%\n", utilizationRate));
            }
            
            // Log the exact context being sent to AI for debugging
            log.info("[Admin AI Summary] Sending to AI:\n{}", userContext.toString());

            List<ChatMessage> messages = List.of(
                    new ChatMessage("user", userContext.toString())
            );

            String geminiSummary = geminiService.generateResponse(systemPrompt, messages);
            if (geminiSummary != null && !geminiSummary.isBlank()) {
                log.info("[Admin AI Summary] AI Response: {}", geminiSummary);
                return geminiSummary;
            }
        } catch (Exception e) {
            log.error("[AdminDashboard] Gemini summary failed, using fallback summary", e);
        }
        
        return fallback.toString();
    }

    // ==================== Staff Dashboard Helpers ====================

    private StaffDashboardResponse.OperationalSummary buildOperationalSummary(List<VehicleAssignmentEntity> assignments) {
        // Use the filtered assignments (already filtered by date range)
        long activeTrips = assignments.stream()
                .filter(a -> "ACTIVE".equals(a.getStatus()) || "IN_PROGRESS".equals(a.getStatus()) || "PICKING_UP".equals(a.getStatus()))
                .count();
        
        long completedTrips = assignments.stream()
                .filter(a -> "COMPLETED".equals(a.getStatus()))
                .count();
        
        // Count delayed trips (would need proper implementation based on your business logic)
        long delayedTrips = assignments.stream()
                .filter(a -> "DELAYED".equals(a.getStatus()))
                .count();

        return StaffDashboardResponse.OperationalSummary.builder()
                .totalTrips((long) assignments.size())
                .activeTrips(activeTrips)
                .completedTrips(completedTrips)
                .delayedTrips(delayedTrips)
                .totalOrderDetails(0L)
                .pendingOrderDetails(0L)
                .deliveringOrderDetails(0L)
                .completedOrderDetails(0L)
                .build();
    }

    private List<StaffDashboardResponse.TripAlert> buildTripAlerts(List<VehicleAssignmentEntity> assignments, List<IssueEntity> issues) {
        List<StaffDashboardResponse.TripAlert> alerts = new ArrayList<>();
        
        // Get assignments with issues (OPEN or IN_PROGRESS)
        for (IssueEntity issue : issues) {
            if (issue.getVehicleAssignmentEntity() != null && 
                    ("OPEN".equals(issue.getStatus()) || "IN_PROGRESS".equals(issue.getStatus()))) {
                VehicleAssignmentEntity assignment = issue.getVehicleAssignmentEntity();
                alerts.add(StaffDashboardResponse.TripAlert.builder()
                        .tripId(assignment.getId().toString())
                        .trackingCode(assignment.getTrackingCode())
                        .vehiclePlate(assignment.getVehicleEntity() != null ? assignment.getVehicleEntity().getLicensePlateNumber() : "")
                        .driverName(assignment.getDriver1() != null && assignment.getDriver1().getUser() != null ? assignment.getDriver1().getUser().getFullName() : "")
                        .status(assignment.getStatus())
                        .alertType("ISSUE_REPORTED")
                        .message(issue.getDescription())
                        .issueId(issue.getId().toString()) // Add issueId for navigation
                        .build());
            }
        }
        
        return alerts;
    }

    private StaffDashboardResponse.IssueSummary buildIssueSummary(List<IssueEntity> issues) {
        Map<String, Long> issuesByCategory = issues.stream()
                .filter(i -> i.getIssueTypeEntity() != null)
                .collect(Collectors.groupingBy(
                        i -> i.getIssueTypeEntity().getIssueTypeName(),
                        Collectors.counting()));

        return StaffDashboardResponse.IssueSummary.builder()
                .totalIssues((long) issues.size())
                .openIssues(issues.stream().filter(i -> "OPEN".equals(i.getStatus()) || "REPORTED".equals(i.getStatus())).count())
                .inProgressIssues(issues.stream().filter(i -> "IN_PROGRESS".equals(i.getStatus())).count())
                .resolvedIssues(issues.stream().filter(i -> "RESOLVED".equals(i.getStatus()) || "CLOSED".equals(i.getStatus())).count())
                .pendingRefunds(0L)
                .issuesByCategory(issuesByCategory)
                .build();
    }

    private List<StaffDashboardResponse.IssueItem> buildPendingIssues(List<IssueEntity> issues) {
        // Only get issues with status OPEN or IN_PROGRESS (as per requirement)
        return issues.stream()
                .filter(i -> "OPEN".equals(i.getStatus()) || "IN_PROGRESS".equals(i.getStatus()))
                .sorted((a, b) -> {
                    // Sort by urgency (OPEN first), then by reportedAt desc
                    if ("OPEN".equals(a.getStatus()) && !"OPEN".equals(b.getStatus())) return -1;
                    if (!"OPEN".equals(a.getStatus()) && "OPEN".equals(b.getStatus())) return 1;
                    if (a.getReportedAt() == null) return 1;
                    if (b.getReportedAt() == null) return -1;
                    return b.getReportedAt().compareTo(a.getReportedAt());
                })
                .map(i -> {
                    String orderCode = "";
                    if (i.getOrderDetails() != null && !i.getOrderDetails().isEmpty()) {
                        OrderDetailEntity firstDetail = i.getOrderDetails().get(0);
                        if (firstDetail.getOrderEntity() != null) {
                            orderCode = firstDetail.getOrderEntity().getOrderCode();
                        }
                    }
                    return StaffDashboardResponse.IssueItem.builder()
                            .issueId(i.getId().toString())
                            .description(i.getDescription())
                            .category(i.getIssueTypeEntity() != null ? i.getIssueTypeEntity().getIssueTypeName() : "")
                            .status(i.getStatus())
                            .reportedAt(i.getReportedAt() != null ? i.getReportedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "")
                            .tripTrackingCode(i.getVehicleAssignmentEntity() != null ? i.getVehicleAssignmentEntity().getTrackingCode() : "")
                            .orderCode(orderCode)
                            .isUrgent("OPEN".equals(i.getStatus()))
                            .build();
                })
                .collect(Collectors.toList());
    }

    private StaffDashboardResponse.FinancialSummary buildFinancialSummary(
            List<ContractEntity> contracts, 
            List<TransactionEntity> transactions,
            List<RefundEntity> refunds) {
        
        long pendingContracts = contracts.stream().filter(c -> "CONTRACT_DRAFT".equals(c.getStatus())).count();
        long paidContracts = contracts.stream().filter(c -> "PAID".equals(c.getStatus())).count();
        long completedContracts = contracts.stream().filter(c -> "COMPLETED".equals(c.getStatus())).count();
        
        BigDecimal totalContractValue = contracts.stream()
                .map(c -> c.getTotalValue() != null ? c.getTotalValue() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        long pendingTransactions = transactions.stream().filter(t -> "PENDING".equals(t.getStatus())).count();
        long completedTransactions = transactions.stream().filter(t -> "PAID".equals(t.getStatus())).count();
        
        BigDecimal transactionAmount = transactions.stream()
                .filter(t -> "PAID".equals(t.getStatus()))
                .map(t -> t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Calculate total refunded amount (completed refunds with refundDate != null)
        BigDecimal totalRefunded = refunds.stream()
                .filter(r -> r.getRefundDate() != null)
                .map(r -> r.getRefundAmount() != null ? r.getRefundAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return StaffDashboardResponse.FinancialSummary.builder()
                .totalContracts((long) contracts.size())
                .pendingContracts(pendingContracts)
                .paidContracts(paidContracts)
                .completedContracts(completedContracts)
                .totalContractValue(totalContractValue)
                .totalTransactions((long) transactions.size())
                .pendingTransactions(pendingTransactions)
                .completedTransactions(completedTransactions)
                .transactionAmount(transactionAmount)
                .totalRefunded(totalRefunded)
                .build();
    }

    private StaffDashboardResponse.FleetStatus buildFleetStatus(List<VehicleEntity> vehicles, List<VehicleServiceRecordEntity> maintenances) {
        long available = vehicles.stream().filter(v -> "ACTIVE".equals(v.getStatus())).count();
        long inUse = vehicles.stream().filter(v -> "IN_TRANSIT".equals(v.getStatus())).count();
        long inMaintenance = vehicles.stream().filter(v -> "MAINTENANCE".equals(v.getStatus())).count();

        LocalDateTime now = LocalDateTime.now();
        // Get all maintenance alerts within next 30 days or overdue
        // Sort: overdue first, then by scheduledDate ascending (nearest first)
        List<StaffDashboardResponse.MaintenanceAlert> alerts = maintenances.stream()
                .filter(m -> m.getPlannedDate() != null && m.getPlannedDate().toLocalDate().isBefore(now.toLocalDate().plusDays(30)))
                .sorted((a, b) -> {
                    boolean aOverdue = a.getPlannedDate().toLocalDate().isBefore(now.toLocalDate());
                    boolean bOverdue = b.getPlannedDate().toLocalDate().isBefore(now.toLocalDate());
                    // Overdue items first
                    if (aOverdue && !bOverdue) return -1;
                    if (!aOverdue && bOverdue) return 1;
                    // Then sort by date ascending (nearest first)
                    return a.getPlannedDate().compareTo(b.getPlannedDate());
                })
                .map(m -> StaffDashboardResponse.MaintenanceAlert.builder()
                        .vehicleId(m.getVehicleEntity() != null ? m.getVehicleEntity().getId().toString() : "")
                        .licensePlate(m.getVehicleEntity() != null ? m.getVehicleEntity().getLicensePlateNumber() : "")
                        .maintenanceType(m.getServiceType() != null ? m.getServiceType() : "")
                        .scheduledDate(m.getPlannedDate().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE))
                        .status(m.getServiceStatus() != null ? m.getServiceStatus().name() : "")
                        .isOverdue(m.getPlannedDate().toLocalDate().isBefore(now.toLocalDate()))
                        .build())
                .collect(Collectors.toList());

        return StaffDashboardResponse.FleetStatus.builder()
                .totalVehicles((long) vehicles.size())
                .availableVehicles(available)
                .inUseVehicles(inUse)
                .inMaintenanceVehicles(inMaintenance)
                .maintenanceAlerts(alerts)
                .fuelAlerts(new ArrayList<>())
                .build();
    }

    private String generateStaffAiSummary(
            StaffDashboardResponse.OperationalSummary ops,
            StaffDashboardResponse.IssueSummary issues,
            StaffDashboardResponse.FinancialSummary financial,
            StaffDashboardResponse.FleetStatus fleet,
            DashboardFilterRequest filter) {
        
        String periodLabel = getPeriodLabel(filter);

        // Fallback summary (hiện đang dùng) phòng khi Gemini lỗi hoặc không cấu hình
        StringBuilder fallback = new StringBuilder();
        fallback.append(String.format("📊 **Vận hành**: %s có **%d chuyến** (", 
                periodLabel, ops.getTotalTrips()));
        
        List<String> tripStats = new ArrayList<>();
        if (ops.getActiveTrips() > 0) tripStats.add(ops.getActiveTrips() + " đang hoạt động");
        if (ops.getCompletedTrips() > 0) tripStats.add(ops.getCompletedTrips() + " hoàn thành");
        if (ops.getDelayedTrips() > 0) tripStats.add("⚠️ " + ops.getDelayedTrips() + " trễ hẹn");
        
        fallback.append(String.join(", ", tripStats)).append("). ");
        
        long totalOpenIssues = issues.getOpenIssues() + issues.getInProgressIssues();
        if (totalOpenIssues > 0) {
            fallback.append(String.format("🚨 **Sự cố**: **%d** cần xử lý (%d mở, %d đang xử lý", 
                    totalOpenIssues, issues.getOpenIssues(), issues.getInProgressIssues()));
            if (issues.getResolvedIssues() > 0) {
                fallback.append(String.format(", %d đã giải quyết", issues.getResolvedIssues()));
            }
            fallback.append("). ");
        } else {
            fallback.append("✅ **Sự cố**: Không có sự cố cần xử lý. ");
        }
        
        fallback.append(String.format("🚛 **Đội xe**: **%d/%d** xe sẵn sàng", 
                fleet.getAvailableVehicles(), fleet.getTotalVehicles()));
        
        // Gọi Gemini để tạo tóm tắt giàu ngữ cảnh hơn
        try {
            String systemPrompt = "Bạn là trợ lý phân tích cho nhân viên vận hành của hệ thống quản lý vận tải. " +
                    "Hãy tóm tắt ngắn gọn (4-6 câu) tình hình vận hành trong " + periodLabel +
                    " bằng tiếng Việt, giọng chuyên nghiệp, tập trung vào hiệu quả và vấn đề cần giải quyết. " +
                    "Phải phân tích TẤT CẢ các số liệu sau: tổng chuyến, trạng thái chuyến (hoạt động/hoàn thành/trễ), " +
                    "sự cố (mở/đang xử lý/đã giải quyết), tài chính (doanh thu, hợp đồng, giao dịch chờ), đội xe (sẵn sàng/hoạt động/bảo dưỡng), " +
                    "VÀ QUAN TRỌNG NHẤT: các cảnh báo bảo dưỡng và vấn đề cần ưu tiên. " +
                    "Nếu có sự cố hoặc xe quá hạn bảo dưỡng, hãy nhấn mạnh và đề xuất hành động. " +
                    "Nếu mọi thứ hoạt động tốt, có thể ghi nhận hiệu suất tích cực. " +
                    "QUAN TRỌNG: Sử dụng định dạng **in đậm** cho tất cả các số liệu quan trọng, tỷ lệ phần trăm, và những điểm cần chú ý đặc biệt.";

            StringBuilder userContext = new StringBuilder();
            userContext.append("=== PHÂN TÍCH DỮ LIỆU DASHBOARD NHÂN VIÊN ===\n");
            userContext.append("=== VẬN HÀNH ===\n");
            userContext.append(String.format("• Tổng chuyến: %d\n", ops.getTotalTrips()));
            userContext.append(String.format("• Chuyến đang hoạt động: %d\n", ops.getActiveTrips()));
            userContext.append(String.format("• Chuyến đã hoàn thành: %d\n", ops.getCompletedTrips()));
            userContext.append(String.format("• Chuyến trễ hẹn: %d\n", ops.getDelayedTrips()));
            if (ops.getTotalTrips() > 0) {
                double completionRate = (double) ops.getCompletedTrips() / ops.getTotalTrips() * 100;
                userContext.append(String.format("• Tỷ lệ hoàn thành: %.1f%%\n", completionRate));
            }
            userContext.append("=== SỰ CỐ ===\n");
            userContext.append(String.format("• Sự cố đang mở: %d\n", issues.getOpenIssues()));
            userContext.append(String.format("• Sự cố đang xử lý: %d\n", issues.getInProgressIssues()));
            userContext.append(String.format("• Sự cố đã giải quyết: %d\n", issues.getResolvedIssues()));
            userContext.append(String.format("• Tổng sự cố cần xử lý: %d\n", totalOpenIssues));
            userContext.append("=== TÀI CHÍNH ===\n");
            if (financial.getTransactionAmount() != null) {
                userContext.append(String.format("• Doanh thu: %s VNĐ\n", financial.getTransactionAmount().toString()));
            }
            userContext.append(String.format("• Hợp đồng chờ ký: %d\n", financial.getPendingContracts()));
            userContext.append(String.format("• Hợp đồng đã thanh toán: %d\n", financial.getPaidContracts()));
            if (financial.getTotalRefunded() != null && financial.getTotalRefunded().compareTo(BigDecimal.ZERO) > 0) {
                userContext.append(String.format("• Tổng đền bù: %s VNĐ\n", financial.getTotalRefunded().toString()));
            }
            userContext.append("=== ĐỘI XE ===\n");
            userContext.append(String.format("• Tổng xe: %d\n", fleet.getTotalVehicles()));
            userContext.append(String.format("• Xe sẵn sàng: %d\n", fleet.getAvailableVehicles()));
            userContext.append(String.format("• Xe đang hoạt động: %d\n", fleet.getInUseVehicles()));
            userContext.append(String.format("• Xe đang bảo dưỡng: %d\n", fleet.getInMaintenanceVehicles()));
            userContext.append("=== CẢNH BÁO BẢO DƯỠNG ===\n");
            if (fleet.getMaintenanceAlerts() != null && !fleet.getMaintenanceAlerts().isEmpty()) {
                long overdue = fleet.getMaintenanceAlerts().stream()
                        .filter(StaffDashboardResponse.MaintenanceAlert::isOverdue)
                        .count();
                long upcoming = fleet.getMaintenanceAlerts().size() - overdue;
                userContext.append(String.format("• Xe quá hạn bảo dưỡng/đăng kiểm: %d\n", overdue));
                userContext.append(String.format("• Xe sắp đến hạn: %d\n", upcoming));
            } else {
                userContext.append("• Không có cảnh báo bảo dưỡng\n");
            }
            
            // Log the exact context being sent to AI for debugging
            log.info("[Staff AI Summary] Sending to AI:\n{}", userContext.toString());

            List<ChatMessage> messages = List.of(
                    new ChatMessage("user", userContext.toString())
            );

            String geminiSummary = geminiService.generateResponse(systemPrompt, messages);
            if (geminiSummary != null && !geminiSummary.isBlank()) {
                log.info("[Staff AI Summary] AI Response: {}", geminiSummary);
                return geminiSummary;
            }
        } catch (Exception e) {
            log.error("[StaffDashboard] Gemini summary failed, using fallback summary", e);
        }

        return fallback.toString().trim();
    }

    // ==================== NEW Staff Dashboard Helpers ====================

    private StaffDashboardResponse.PackageSummary buildStaffPackageSummary(List<OrderEntity> orders, List<OrderDetailEntity> orderDetails) {
        long inTransit = orderDetails.stream()
                .filter(od -> "PICKING_UP".equals(od.getStatus()) || "ON_DELIVERED".equals(od.getStatus()) || "ONGOING_DELIVERED".equals(od.getStatus()))
                .count();
        
        long delivered = orderDetails.stream()
                .filter(od -> "DELIVERED".equals(od.getStatus()) || "SUCCESSFUL".equals(od.getStatus()))
                .count();
        
        long cancelled = orderDetails.stream()
                .filter(od -> "CANCELLED".equals(od.getStatus()))
                .count();
        
        long problem = orderDetails.stream()
                .filter(od -> "IN_TROUBLES".equals(od.getStatus()) || "COMPENSATION".equals(od.getStatus()) || 
                             "RETURNING".equals(od.getStatus()) || "RETURNED".equals(od.getStatus()))
                .count();
        
        long total = delivered + cancelled + problem;
        double successRate = total > 0 ? (double) delivered / total * 100 : 0.0;

        return StaffDashboardResponse.PackageSummary.builder()
                .totalOrderDetails((long) orderDetails.size())
                .inTransitPackages(inTransit)
                .deliveredPackages(delivered)
                .cancelledPackages(cancelled)
                .problemPackages(problem)
                .totalOrders((long) orders.size())
                .successRate(Math.round(successRate * 100.0) / 100.0)
                .build();
    }

    private List<StaffDashboardResponse.TripCompletionTrend> buildTripCompletionTrend(
            List<VehicleAssignmentEntity> assignments, DashboardFilterRequest filter) {
        
        DashboardFilterRequest.TimeRange range = filter.getRange();
        
        // Generate all date labels for the time range
        List<String> allDateLabels = generateAllDateLabels(filter);
        
        // Group assignments by date key
        Map<String, List<VehicleAssignmentEntity>> assignmentsByDate = assignments.stream()
                .filter(a -> a.getCreatedAt() != null)
                .collect(Collectors.groupingBy(a -> getDateKey(a.getCreatedAt(), filter)));
        
        // Build trend data with all date points (including empty ones)
        return allDateLabels.stream()
                .map(dateLabel -> {
                    List<VehicleAssignmentEntity> list = assignmentsByDate.getOrDefault(dateLabel, Collections.emptyList());
                    long completed = list.stream().filter(a -> "COMPLETED".equals(a.getStatus())).count();
                    long active = list.stream().filter(a -> "ACTIVE".equals(a.getStatus()) || "IN_PROGRESS".equals(a.getStatus())).count();
                    
                    return StaffDashboardResponse.TripCompletionTrend.builder()
                            .date(dateLabel)
                            .completedTrips(completed)
                            .activeTrips(active)
                            .totalTrips((long) list.size())
                            .build();
                })
                .collect(Collectors.toList());
    }

    private List<StaffDashboardResponse.IssueTypeTrend> buildIssueTypeTrend(
            List<IssueEntity> issues, DashboardFilterRequest filter) {
        
        // Generate all date labels for the time range
        List<String> allDateLabels = generateAllDateLabels(filter);
        
        // Get all unique issue types
        Set<String> allIssueTypes = issues.stream()
                .filter(i -> i.getIssueTypeEntity() != null)
                .map(i -> i.getIssueTypeEntity().getIssueTypeName())
                .collect(Collectors.toSet());
        
        // Group by date and issue type
        Map<String, Map<String, Long>> issuesByDateAndType = issues.stream()
                .filter(i -> i.getReportedAt() != null && i.getIssueTypeEntity() != null)
                .collect(Collectors.groupingBy(
                        i -> getDateKey(i.getReportedAt(), filter),
                        Collectors.groupingBy(
                                i -> i.getIssueTypeEntity().getIssueTypeName(),
                                Collectors.counting()
                        )
                ));
        
        // Build trend data with all date points (including empty ones)
        List<StaffDashboardResponse.IssueTypeTrend> result = new ArrayList<>();
        for (String dateLabel : allDateLabels) {
            Map<String, Long> typeCountsForDate = issuesByDateAndType.getOrDefault(dateLabel, Collections.emptyMap());
            for (String issueType : allIssueTypes) {
                result.add(StaffDashboardResponse.IssueTypeTrend.builder()
                        .date(dateLabel)
                        .issueType(issueType)
                        .count(typeCountsForDate.getOrDefault(issueType, 0L))
                        .build());
            }
        }
        
        return result;
    }

    private List<StaffDashboardResponse.ContractTrend> buildStaffContractTrend(
            List<ContractEntity> contracts, DashboardFilterRequest filter) {
        
        // Generate all date labels for the time range
        List<String> allDateLabels = generateAllDateLabels(filter);
        
        // Group contracts by date key
        Map<String, List<ContractEntity>> contractsByDate = contracts.stream()
                .filter(c -> c.getCreatedAt() != null)
                .collect(Collectors.groupingBy(c -> getDateKey(c.getCreatedAt(), filter)));
        
        // Build trend data with all date points (including empty ones)
        return allDateLabels.stream()
                .map(dateLabel -> {
                    List<ContractEntity> list = contractsByDate.getOrDefault(dateLabel, Collections.emptyList());
                    long paid = list.stream().filter(c -> "PAID".equals(c.getStatus())).count();
                    long cancelled = list.stream().filter(c -> "CANCELLED".equals(c.getStatus())).count();
                    BigDecimal totalValue = list.stream()
                            .map(c -> c.getTotalValue() != null ? c.getTotalValue() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    
                    return StaffDashboardResponse.ContractTrend.builder()
                            .date(dateLabel)
                            .createdCount((long) list.size())
                            .paidCount(paid)
                            .cancelledCount(cancelled)
                            .totalValue(totalValue)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private List<StaffDashboardResponse.TransactionTrend> buildStaffTransactionTrend(
            List<TransactionEntity> transactions, DashboardFilterRequest filter) {
        
        // Generate all date labels for the time range
        List<String> allDateLabels = generateAllDateLabels(filter);
        
        // Group transactions by date key
        Map<String, List<TransactionEntity>> transactionsByDate = transactions.stream()
                .filter(t -> t.getPaymentDate() != null && "PAID".equals(t.getStatus()))
                .collect(Collectors.groupingBy(t -> getDateKey(t.getPaymentDate(), filter)));
        
        // Build trend data with all date points (including empty ones)
        return allDateLabels.stream()
                .map(dateLabel -> {
                    List<TransactionEntity> list = transactionsByDate.getOrDefault(dateLabel, Collections.emptyList());
                    BigDecimal totalAmount = list.stream()
                            .map(t -> t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    
                    return StaffDashboardResponse.TransactionTrend.builder()
                            .date(dateLabel)
                            .paidAmount(totalAmount)
                            .paidCount((long) list.size())
                            .build();
                })
                .collect(Collectors.toList());
    }

    private List<StaffDashboardResponse.RefundTrend> buildRefundTrend(
            List<RefundEntity> refunds, DashboardFilterRequest filter) {
        
        // Generate all date labels for the time range
        List<String> allDateLabels = generateAllDateLabels(filter);
        
        // Group refunds by date key
        Map<String, List<RefundEntity>> refundsByDate = refunds.stream()
                .filter(r -> r.getRefundDate() != null)
                .collect(Collectors.groupingBy(r -> getDateKey(r.getRefundDate(), filter)));
        
        // Build trend data with all date points (including empty ones)
        return allDateLabels.stream()
                .map(dateLabel -> {
                    List<RefundEntity> list = refundsByDate.getOrDefault(dateLabel, Collections.emptyList());
                    BigDecimal totalAmount = list.stream()
                            .map(r -> r.getRefundAmount() != null ? r.getRefundAmount() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    
                    return StaffDashboardResponse.RefundTrend.builder()
                            .date(dateLabel)
                            .refundCount((long) list.size())
                            .refundAmount(totalAmount)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private List<StaffDashboardResponse.RevenueCompensationTrend> buildRevenueCompensationTrend(
            List<TransactionEntity> transactions, List<RefundEntity> refunds, DashboardFilterRequest filter) {
        
        // Generate all date labels for the time range
        List<String> allDateLabels = generateAllDateLabels(filter);
        
        // Group transactions by date
        Map<String, BigDecimal> revenueByDate = transactions.stream()
                .filter(t -> t.getPaymentDate() != null && "PAID".equals(t.getStatus()))
                .collect(Collectors.groupingBy(
                        t -> getDateKey(t.getPaymentDate(), filter),
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                t -> t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO,
                                BigDecimal::add
                        )
                ));
        
        // Group refunds by date
        Map<String, BigDecimal> compensationByDate = refunds.stream()
                .filter(r -> r.getRefundDate() != null)
                .collect(Collectors.groupingBy(
                        r -> getDateKey(r.getRefundDate(), filter),
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                r -> r.getRefundAmount() != null ? r.getRefundAmount() : BigDecimal.ZERO,
                                BigDecimal::add
                        )
                ));
        
        // Build trend data with all date points (including empty ones)
        return allDateLabels.stream()
                .map(dateLabel -> StaffDashboardResponse.RevenueCompensationTrend.builder()
                        .date(dateLabel)
                        .revenue(revenueByDate.getOrDefault(dateLabel, BigDecimal.ZERO))
                        .compensation(compensationByDate.getOrDefault(dateLabel, BigDecimal.ZERO))
                        .build())
                .collect(Collectors.toList());
    }

    private List<StaffDashboardResponse.PackageStatusTrend> buildStaffPackageStatusTrend(
            List<OrderDetailEntity> orderDetails, DashboardFilterRequest filter) {
        
        // Generate all date labels for the time range
        List<String> allDateLabels = generateAllDateLabels(filter);
        
        // Group order details by date key
        Map<String, List<OrderDetailEntity>> detailsByDate = orderDetails.stream()
                .filter(od -> od.getCreatedAt() != null)
                .collect(Collectors.groupingBy(od -> getDateKey(od.getCreatedAt(), filter)));
        
        // Build trend data with all date points (including empty ones)
        return allDateLabels.stream()
                .map(dateLabel -> {
                    List<OrderDetailEntity> details = detailsByDate.getOrDefault(dateLabel, Collections.emptyList());
                    
                    long inTransit = details.stream()
                            .filter(od -> "PICKING_UP".equals(od.getStatus()) || 
                                         "ON_DELIVERED".equals(od.getStatus()) || 
                                         "ONGOING_DELIVERED".equals(od.getStatus()))
                            .count();
                    
                    long delivered = details.stream()
                            .filter(od -> "DELIVERED".equals(od.getStatus()) || "SUCCESSFUL".equals(od.getStatus()))
                            .count();
                    
                    long cancelled = details.stream()
                            .filter(od -> "CANCELLED".equals(od.getStatus()))
                            .count();
                    
                    long problem = details.stream()
                            .filter(od -> "IN_TROUBLES".equals(od.getStatus()) || 
                                         "COMPENSATION".equals(od.getStatus()) || 
                                         "RETURNING".equals(od.getStatus()) || 
                                         "RETURNED".equals(od.getStatus()))
                            .count();
                    
                    return StaffDashboardResponse.PackageStatusTrend.builder()
                            .date(dateLabel)
                            .inTransit(inTransit)
                            .delivered(delivered)
                            .cancelled(cancelled)
                            .problem(problem)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private List<StaffDashboardResponse.RecentOrderItem> buildStaffRecentOrders(List<OrderEntity> orders) {
        return orders.stream()
                .sorted((a, b) -> {
                    if (a.getCreatedAt() == null) return 1;
                    if (b.getCreatedAt() == null) return -1;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .limit(20)
                .map(o -> {
                    long totalPackages = o.getOrderDetailEntities() != null ? o.getOrderDetailEntities().size() : 0;
                    long deliveredPackages = o.getOrderDetailEntities() != null ? 
                            o.getOrderDetailEntities().stream()
                                    .filter(od -> "DELIVERED".equals(od.getStatus()) || "SUCCESSFUL".equals(od.getStatus()))
                                    .count() : 0;
                    boolean hasIssue = o.getOrderDetailEntities() != null && 
                            o.getOrderDetailEntities().stream().anyMatch(od -> od.getIssueEntity() != null);
                    
                    return StaffDashboardResponse.RecentOrderItem.builder()
                            .orderId(o.getId().toString())
                            .orderCode(o.getOrderCode())
                            .senderName(o.getSender() != null && o.getSender().getUser() != null ? 
                                    o.getSender().getUser().getFullName() : "")
                            .senderCompany(o.getSender() != null ? o.getSender().getCompanyName() : "")
                            .totalPackages(totalPackages)
                            .deliveredPackages(deliveredPackages)
                            .status(o.getStatus())
                            .hasIssue(hasIssue)
                            .createdAt(o.getCreatedAt() != null ? o.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "")
                            .build();
                })
                .collect(Collectors.toList());
    }

    private List<StaffDashboardResponse.TopCustomerItem> buildTopCustomers(List<OrderEntity> orders, List<TransactionEntity> transactions) {
        // Group orders by customer
        Map<UUID, List<OrderEntity>> ordersByCustomer = orders.stream()
                .filter(o -> o.getSender() != null)
                .collect(Collectors.groupingBy(o -> o.getSender().getId()));
        
        return ordersByCustomer.entrySet().stream()
                .map(e -> {
                    UUID customerId = e.getKey();
                    List<OrderEntity> customerOrders = e.getValue();
                    
                    // Get customer info from first order
                    OrderEntity firstOrder = customerOrders.get(0);
                    String customerName = firstOrder.getSender().getUser() != null ? 
                            firstOrder.getSender().getUser().getFullName() : "";
                    String companyName = firstOrder.getSender().getCompanyName();
                    
                    // Calculate package stats
                    long totalPackages = customerOrders.stream()
                            .mapToLong(o -> o.getOrderDetailEntities() != null ? o.getOrderDetailEntities().size() : 0)
                            .sum();
                    
                    long deliveredPackages = customerOrders.stream()
                            .flatMap(o -> o.getOrderDetailEntities() != null ? o.getOrderDetailEntities().stream() : java.util.stream.Stream.empty())
                            .filter(od -> "DELIVERED".equals(od.getStatus()) || "SUCCESSFUL".equals(od.getStatus()))
                            .count();
                    
                    double successRate = totalPackages > 0 ? (double) deliveredPackages / totalPackages * 100 : 0.0;
                    
                    // Calculate revenue from transactions
                    BigDecimal totalRevenue = customerOrders.stream()
                            .flatMap(o -> {
                                try {
                                    return contractEntityService.getContractByOrderId(o.getId())
                                            .map(c -> transactionEntityService.findByContractId(c.getId()).stream())
                                            .orElse(java.util.stream.Stream.empty());
                                } catch (Exception ex) {
                                    return java.util.stream.Stream.empty();
                                }
                            })
                            .filter(t -> "PAID".equals(t.getStatus()))
                            .map(t -> t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    
                    return StaffDashboardResponse.TopCustomerItem.builder()
                            .customerId(customerId.toString())
                            .customerName(customerName)
                            .companyName(companyName)
                            .totalOrders((long) customerOrders.size())
                            .totalPackages(totalPackages)
                            .totalRevenue(totalRevenue)
                            .successRate(Math.round(successRate * 100.0) / 100.0)
                            .build();
                })
                .sorted(Comparator.comparing(StaffDashboardResponse.TopCustomerItem::getTotalRevenue).reversed())
                .limit(5)
                .collect(Collectors.toList());
    }

    private List<StaffDashboardResponse.TopDriverItem> buildTopDrivers(List<VehicleAssignmentEntity> assignments) {
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
                    String driverName = firstAssignment.getDriver1().getUser() != null ? 
                            firstAssignment.getDriver1().getUser().getFullName() : "";
                    String phone = firstAssignment.getDriver1().getUser() != null ? 
                            firstAssignment.getDriver1().getUser().getPhoneNumber() : "";
                    
                    // Calculate trip stats
                    long totalTrips = driverAssignments.size();
                    long completedTrips = driverAssignments.stream()
                            .filter(a -> "COMPLETED".equals(a.getStatus()))
                            .count();
                    
                    // For on-time calculation, we'll use a simple approach based on status
                    // Since we don't have actual/expected end times in the entity
                    long onTimeTrips = completedTrips; // Assume all completed trips are on time for now
                    
                    double completionRate = totalTrips > 0 ? (double) completedTrips / totalTrips * 100 : 0.0;
                    double onTimePercentage = completedTrips > 0 ? 100.0 : 0.0; // Simplified for now
                    
                    return StaffDashboardResponse.TopDriverItem.builder()
                            .driverId(driverId.toString())
                            .driverName(driverName)
                            .phone(phone)
                            .completedTrips(completedTrips)
                            .totalTrips(totalTrips)
                            .onTimePercentage(Math.round(onTimePercentage * 100.0) / 100.0)
                            .completionRate(Math.round(completionRate * 100.0) / 100.0)
                            .build();
                })
                .sorted(Comparator.comparing(StaffDashboardResponse.TopDriverItem::getCompletedTrips).reversed())
                .limit(5)
                .collect(Collectors.toList());
    }

    private List<StaffDashboardResponse.PendingOrderItem> buildPendingOrders(List<OrderEntity> orders) {
        // Get orders with PROCESSING or ON_PLANNING status
        return orders.stream()
                .filter(o -> "PROCESSING".equals(o.getStatus()) || "ON_PLANNING".equals(o.getStatus()))
                .sorted(Comparator.comparing(OrderEntity::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(20)
                .map(o -> {
                    long totalPackages = o.getOrderDetailEntities() != null ? o.getOrderDetailEntities().size() : 0;
                    
                    String pickupAddress = o.getPickupAddress() != null ? 
                            String.format("%s, %s, %s", 
                                    o.getPickupAddress().getStreet() != null ? o.getPickupAddress().getStreet() : "",
                                    o.getPickupAddress().getWard() != null ? o.getPickupAddress().getWard() : "",
                                    o.getPickupAddress().getProvince() != null ? o.getPickupAddress().getProvince() : "") : "";
                    
                    String deliveryAddress = o.getDeliveryAddress() != null ? 
                            String.format("%s, %s, %s", 
                                    o.getDeliveryAddress().getStreet() != null ? o.getDeliveryAddress().getStreet() : "",
                                    o.getDeliveryAddress().getWard() != null ? o.getDeliveryAddress().getWard() : "",
                                    o.getDeliveryAddress().getProvince() != null ? o.getDeliveryAddress().getProvince() : "") : "";
                    
                    return StaffDashboardResponse.PendingOrderItem.builder()
                            .orderId(o.getId().toString())
                            .orderCode(o.getOrderCode())
                            .senderName(o.getSender() != null && o.getSender().getUser() != null ? 
                                    o.getSender().getUser().getFullName() : "")
                            .senderCompany(o.getSender() != null ? o.getSender().getCompanyName() : "")
                            .senderPhone(o.getSender() != null && o.getSender().getUser() != null ? 
                                    o.getSender().getUser().getPhoneNumber() : "")
                            .totalPackages(totalPackages)
                            .status(o.getStatus())
                            .pickupAddress(pickupAddress)
                            .deliveryAddress(deliveryAddress)
                            .createdAt(o.getCreatedAt() != null ? o.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "")
                            .note(o.getNotes())
                            .build();
                })
                .collect(Collectors.toList());
    }

    // ==================== Customer Dashboard Helpers ====================

    private CustomerDashboardResponse.OrderSummary buildCustomerOrderSummary(List<OrderEntity> orders, List<OrderDetailEntity> orderDetails) {
        // Package statistics by status
        long inTransit = orderDetails.stream()
                .filter(od -> "PICKING_UP".equals(od.getStatus()) || "ON_DELIVERED".equals(od.getStatus()) || "ONGOING_DELIVERED".equals(od.getStatus()))
                .count();
        
        long delivered = orderDetails.stream()
                .filter(od -> "DELIVERED".equals(od.getStatus()) || "SUCCESSFUL".equals(od.getStatus()))
                .count();
        
        long cancelled = orderDetails.stream()
                .filter(od -> "CANCELLED".equals(od.getStatus()))
                .count();
        
        long problem = orderDetails.stream()
                .filter(od -> "IN_TROUBLES".equals(od.getStatus()) || "COMPENSATION".equals(od.getStatus()) || 
                             "RETURNING".equals(od.getStatus()) || "RETURNED".equals(od.getStatus()))
                .count();
        
        // Success rate calculation
        long total = delivered + cancelled + problem;
        double successRate = total > 0 ? (double) delivered / total * 100 : 0.0;
        
        // Order counts for active orders section
        long pending = orders.stream()
                .filter(o -> "PENDING".equals(o.getStatus()))
                .count();
        
        long inProgress = orders.stream()
                .filter(o -> "PROCESSING".equals(o.getStatus()) || "CONTRACT_DRAFT".equals(o.getStatus()) || 
                            "CONTRACT_SIGNED".equals(o.getStatus()) || "ON_PLANNING".equals(o.getStatus()) || 
                            "ASSIGNED_TO_DRIVER".equals(o.getStatus()))
                .count();

        return CustomerDashboardResponse.OrderSummary.builder()
                .totalOrders((long) orders.size())
                .totalOrderDetails((long) orderDetails.size())
                .inTransitPackages(inTransit)
                .deliveredPackages(delivered)
                .cancelledPackages(cancelled)
                .problemPackages(problem)
                .pendingOrders(pending)
                .inProgressOrders(inProgress)
                .successRate(Math.round(successRate * 100.0) / 100.0)
                .build();
    }

    private CustomerDashboardResponse.DeliveryPerformance buildCustomerDeliveryPerformance(
            List<OrderDetailEntity> orderDetails, List<IssueEntity> issues) {
        
        long successful = orderDetails.stream()
                .filter(od -> "DELIVERED".equals(od.getStatus()) || "SUCCESSFUL".equals(od.getStatus()))
                .count();
        
        long failed = orderDetails.stream()
                .filter(od -> "CANCELLED".equals(od.getStatus()) || "IN_TROUBLES".equals(od.getStatus()) || 
                             "COMPENSATION".equals(od.getStatus()) || "RETURNING".equals(od.getStatus()) || 
                             "RETURNED".equals(od.getStatus()))
                .count();
        
        long total = successful + failed;
        double successRate = total > 0 ? (double) successful / total * 100 : 0;
        double issueRate = orderDetails.size() > 0 ? (double) issues.size() / orderDetails.size() * 100 : 0;

        return CustomerDashboardResponse.DeliveryPerformance.builder()
                .successfulDeliveries(successful)
                .failedDeliveries(failed)
                .successRate(Math.round(successRate * 100.0) / 100.0)
                .issueCount((long) issues.size())
                .issueRate(Math.round(issueRate * 100.0) / 100.0)
                .trendData(new ArrayList<>())
                .build();
    }

    private CustomerDashboardResponse.FinancialSummary buildCustomerFinancialSummary(
            List<ContractEntity> allContracts, List<TransactionEntity> allTransactions,
            DashboardFilterRequest filter) {
        
        BigDecimal totalPaid = allTransactions.stream()
                .filter(t -> "PAID".equals(t.getStatus()))
                .map(t -> t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Total contract value = only PAID contracts (completed)
        BigDecimal totalContractValue = allContracts.stream()
                .filter(c -> "PAID".equals(c.getStatus()))
                .map(c -> c.getTotalValue() != null ? c.getTotalValue() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Pending payment = all contracts value - paid amount
        BigDecimal allContractsValue = allContracts.stream()
                .map(c -> c.getTotalValue() != null ? c.getTotalValue() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pendingPayment = allContractsValue.subtract(totalPaid);
        if (pendingPayment.compareTo(BigDecimal.ZERO) < 0) {
            pendingPayment = BigDecimal.ZERO;
        }
        
        // Contract states based on order status
        long contractsPendingSignature = allContracts.stream()
                .filter(c -> "CONTRACT_DRAFT".equals(c.getStatus()))
                .count();
        
        long contractsAwaitingDeposit = allContracts.stream()
                .filter(c -> c.getOrderEntity() != null && "CONTRACT_SIGNED".equals(c.getOrderEntity().getStatus()))
                .count();
        
        long contractsAwaitingFullPayment = allContracts.stream()
                .filter(c -> c.getOrderEntity() != null && "ASSIGNED_TO_DRIVER".equals(c.getOrderEntity().getStatus()))
                .count();
        
        // TODO: Implement logic to check for return fee requirement
        long contractsAwaitingReturnFee = 0L;
        
        long contractsSigned = allContracts.stream()
                .filter(c -> "PAID".equals(c.getStatus()))
                .count();
        
        long contractsCancelled = allContracts.stream()
                .filter(c -> "CANCELLED".equals(c.getStatus()))
                .count();
        
        // Calculate refunds only for this customer's contracts through issue chain
        BigDecimal totalRefunded = allContracts.stream()
                .flatMap(c -> {
                    try {
                        // Get order from contract
                        if (c.getOrderEntity() == null) return java.util.stream.Stream.empty();
                        OrderEntity order = c.getOrderEntity();
                        
                        // Get order details with issues
                        return order.getOrderDetailEntities().stream()
                                .filter(od -> od.getIssueEntity() != null)
                                .map(od -> od.getIssueEntity())
                                .filter(issue -> issue != null)
                                .flatMap(issue -> {
                                    try {
                                        // Get refunds for this issue
                                        return refundEntityService.findByIssueId(issue.getId()).stream();
                                    } catch (Exception ex) {
                                        return java.util.stream.Stream.empty();
                                    }
                                });
                    } catch (Exception ex) {
                        return java.util.stream.Stream.empty();
                    }
                })
                .filter(r -> r.getRefundDate() != null)
                .map(r -> r.getRefundAmount() != null ? r.getRefundAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Contract value trend & transaction trend theo range filter
        List<CustomerDashboardResponse.ContractValueTrend> contractValueTrend =
                buildContractValueTrend(allContracts, filter);
        List<CustomerDashboardResponse.TransactionTrend> transactionTrend =
                buildTransactionTrend(allTransactions, filter);

        return CustomerDashboardResponse.FinancialSummary.builder()
                .totalPaid(totalPaid)
                .pendingPayment(pendingPayment)
                .totalRefunded(totalRefunded)
                .totalContractValue(totalContractValue)
                .contractsPendingSignature(contractsPendingSignature)
                .contractsAwaitingDeposit(contractsAwaitingDeposit)
                .contractsAwaitingFullPayment(contractsAwaitingFullPayment)
                .contractsAwaitingReturnFee(contractsAwaitingReturnFee)
                .contractsSigned(contractsSigned)
                .contractsCancelled(contractsCancelled)
                .contractValueTrend(contractValueTrend)
                .transactionTrend(transactionTrend)
                .build();
    }

    private List<CustomerDashboardResponse.ActiveOrderItem> buildActiveOrders(List<OrderEntity> orders) {
        return orders.stream()
                .filter(o -> !"SUCCESSFUL".equals(o.getStatus()) && !"COMPLETED".equals(o.getStatus()) && !"CANCELLED".equals(o.getStatus()))
                .map(o -> {
                    int totalPackages = o.getOrderDetailEntities().size();
                    int deliveredPackages = (int) o.getOrderDetailEntities().stream()
                            .filter(od -> "SUCCESSFUL".equals(od.getStatus()) || "DELIVERED".equals(od.getStatus()))
                            .count();
                    
                    return CustomerDashboardResponse.ActiveOrderItem.builder()
                            .orderId(o.getId().toString())
                            .orderCode(o.getOrderCode())
                            .status(o.getStatus())
                            .pickupAddress(o.getPickupAddress() != null ? o.getPickupAddress().getStreet() : "")
                            .deliveryAddress(o.getDeliveryAddress() != null ? o.getDeliveryAddress().getStreet() : "")
                            .totalPackages(totalPackages)
                            .deliveredPackages(deliveredPackages)
                            .hasIssue(o.getOrderDetailEntities().stream().anyMatch(od -> od.getIssueEntity() != null))
                            .build();
                })
                .limit(5)
                .collect(Collectors.toList());
    }

    private CustomerDashboardResponse.ActionsSummary buildActionsSummary(
            List<ContractEntity> contracts, List<IssueEntity> issues) {
        
        long contractsToSign = contracts.stream()
                .filter(c -> "CONTRACT_DRAFT".equals(c.getStatus()))
                .count();
        
        long paymentsNeeded = contracts.stream()
                .filter(c -> c.getOrderEntity() != null && 
                        ("CONTRACT_SIGNED".equals(c.getOrderEntity().getStatus()) || 
                         "ASSIGNED_TO_DRIVER".equals(c.getOrderEntity().getStatus())))
                .count();
        
        long issuesNeedingResponse = issues.stream()
                .filter(i -> "OPEN".equals(i.getStatus()) || "AWAITING_CUSTOMER".equals(i.getStatus()))
                .count();

        List<CustomerDashboardResponse.ActionItem> actionItems = new ArrayList<>();
        
        // Contracts to sign (ORDER status = CONTRACT_DRAFT)
        contracts.stream()
                .filter(c -> c.getOrderEntity() != null && "CONTRACT_DRAFT".equals(c.getOrderEntity().getStatus()))
                .forEach(c -> actionItems.add(CustomerDashboardResponse.ActionItem.builder()
                        .type("CONTRACT_SIGN")
                        .id(c.getId().toString())
                        .orderId(c.getOrderEntity().getId().toString())
                        .orderCode(c.getOrderEntity().getOrderCode())
                        .title("Ký hợp đồng & đặt cọc")
                        .description("Đơn " + c.getOrderEntity().getOrderCode() + " - Cần ký hợp đồng và thanh toán cọc")
                        .deadline(c.getSigningDeadline() != null ? c.getSigningDeadline().format(DEADLINE_FORMATTER) : "")
                        .urgency("HIGH")
                        .amount(c.getTotalValue() != null ? c.getTotalValue().multiply(new BigDecimal("0.3")) : BigDecimal.ZERO)
                        .build()));
        
        // Awaiting deposit (ORDER status = CONTRACT_SIGNED)
        contracts.stream()
                .filter(c -> c.getOrderEntity() != null && "CONTRACT_SIGNED".equals(c.getOrderEntity().getStatus()))
                .forEach(c -> actionItems.add(CustomerDashboardResponse.ActionItem.builder()
                        .type("DEPOSIT_PAYMENT")
                        .id(c.getId().toString())
                        .orderId(c.getOrderEntity().getId().toString())
                        .orderCode(c.getOrderEntity().getOrderCode())
                        .title("Thanh toán đặt cọc")
                        .description("Đơn " + c.getOrderEntity().getOrderCode() + " - Cần thanh toán tiền cọc")
                        .deadline(c.getDepositPaymentDeadline() != null ? c.getDepositPaymentDeadline().format(DEADLINE_FORMATTER) : "")
                        .urgency("HIGH")
                        .amount(c.getTotalValue() != null ? c.getTotalValue().multiply(new BigDecimal("0.3")) : BigDecimal.ZERO)
                        .build()));
        
        // Awaiting full payment (ORDER status = ASSIGNED_TO_DRIVER)
        contracts.stream()
                .filter(c -> c.getOrderEntity() != null && "ASSIGNED_TO_DRIVER".equals(c.getOrderEntity().getStatus()))
                .forEach(c -> actionItems.add(CustomerDashboardResponse.ActionItem.builder()
                        .type("FULL_PAYMENT")
                        .id(c.getId().toString())
                        .orderId(c.getOrderEntity().getId().toString())
                        .orderCode(c.getOrderEntity().getOrderCode())
                        .title("Thanh toán toàn bộ")
                        .description("Đơn " + c.getOrderEntity().getOrderCode() + " - Cần thanh toán phần còn lại")
                        .deadline(c.getFullPaymentDeadline() != null ? c.getFullPaymentDeadline().format(DEADLINE_FORMATTER) : "")
                        .urgency("HIGH")
                        .amount(c.getTotalValue() != null ? c.getTotalValue().multiply(new BigDecimal("0.7")) : BigDecimal.ZERO)
                        .build()));

        return CustomerDashboardResponse.ActionsSummary.builder()
                .contractsToSign(contractsToSign)
                .paymentsNeeded(paymentsNeeded)
                .issuesNeedingResponse(issuesNeedingResponse)
                .actionItems(actionItems)
                .build();
    }

    private List<CustomerDashboardResponse.ActivityItem> buildRecentActivity(
            List<OrderEntity> orders, List<TransactionEntity> transactions) {
        
        List<CustomerDashboardResponse.ActivityItem> activities = new ArrayList<>();
        
        orders.stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(5)
                .forEach(o -> activities.add(CustomerDashboardResponse.ActivityItem.builder()
                        .type("ORDER")
                        .title("Đơn hàng " + o.getOrderCode())
                        .description(o.getOrderDetailEntities().size() + " kiện hàng")
                        .timestamp(o.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                        .orderId(o.getId().toString())
                        .relatedOrderCode(o.getOrderCode())
                        .orderStatus(o.getStatus())
                        .build()));
        
        return activities.stream()
                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .limit(10)
                .collect(Collectors.toList());
    }
    
    private List<CustomerDashboardResponse.ContractValueTrend> buildContractValueTrend(
            List<ContractEntity> contracts, DashboardFilterRequest filter) {

        // Generate all date labels for the time range
        List<String> allDateLabels = generateAllDateLabels(filter);

        // Group contracts by date key
        Map<String, List<ContractEntity>> contractsByDate = contracts.stream()
                // Chỉ lấy các hợp đồng đã thanh toán (status = PAID) và có ngày hiệu lực hoặc ngày tạo
                .filter(c -> "PAID".equals(c.getStatus()))
                .filter(c -> c.getEffectiveDate() != null || c.getCreatedAt() != null)
                .collect(Collectors.groupingBy(c -> {
                    LocalDateTime date = c.getEffectiveDate() != null ? c.getEffectiveDate() : c.getCreatedAt();
                    return getDateKey(date, filter);
                }));
        
        // Build trend data with all date points (including empty ones)
        return allDateLabels.stream()
                .map(dateLabel -> {
                    List<ContractEntity> list = contractsByDate.getOrDefault(dateLabel, Collections.emptyList());
                    BigDecimal totalValue = list.stream()
                            .map(c -> {
                                // Prioritize adjustedValue if > 0, otherwise use totalValue
                                BigDecimal adjusted = c.getAdjustedValue();
                                BigDecimal total = c.getTotalValue();
                                if (adjusted != null && adjusted.compareTo(BigDecimal.ZERO) > 0) {
                                    return adjusted;
                                }
                                return total != null ? total : BigDecimal.ZERO;
                            })
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    
                    return CustomerDashboardResponse.ContractValueTrend.builder()
                            .date(dateLabel)
                            .contractCount((long) list.size())
                            .totalValue(totalValue)
                            .build();
                })
                .collect(Collectors.toList());
    }
    
    private List<CustomerDashboardResponse.TransactionTrend> buildTransactionTrend(
            List<TransactionEntity> transactions, DashboardFilterRequest filter) {

        // Generate all date labels for the time range
        List<String> allDateLabels = generateAllDateLabels(filter);

        // Group transactions by date key
        Map<String, List<TransactionEntity>> transactionsByDate = transactions.stream()
                .filter(t -> t.getPaymentDate() != null && "PAID".equals(t.getStatus()))
                .collect(Collectors.groupingBy(t -> getDateKey(t.getPaymentDate(), filter)));
        
        // Build trend data with all date points (including empty ones)
        return allDateLabels.stream()
                .map(dateLabel -> {
                    List<TransactionEntity> list = transactionsByDate.getOrDefault(dateLabel, Collections.emptyList());
                    BigDecimal totalAmount = list.stream()
                            .map(t -> t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    
                    return CustomerDashboardResponse.TransactionTrend.builder()
                            .date(dateLabel)
                            .amount(totalAmount)
                            .transactionCount((long) list.size())
                            .build();
                })
                .collect(Collectors.toList());
    }
    
    private List<CustomerDashboardResponse.PackageStatusTrend> buildPackageStatusTrend(
            List<OrderDetailEntity> orderDetails, DashboardFilterRequest filter) {

        // Generate all date labels for the time range
        List<String> allDateLabels = generateAllDateLabels(filter);
        
        // Group order details by date key
        Map<String, List<OrderDetailEntity>> detailsByDate = orderDetails.stream()
                .filter(od -> od.getCreatedAt() != null)
                .collect(Collectors.groupingBy(od -> getDateKey(od.getCreatedAt(), filter)));

        // Build trend data with all date points (including empty ones)
        return allDateLabels.stream()
                .map(dateLabel -> {
                    List<OrderDetailEntity> details = detailsByDate.getOrDefault(dateLabel, Collections.emptyList());
                    
                    long inTransit = details.stream()
                            .filter(od -> "PICKING_UP".equals(od.getStatus()) || 
                                         "ON_DELIVERED".equals(od.getStatus()) || 
                                         "ONGOING_DELIVERED".equals(od.getStatus()))
                            .count();
                    
                    long delivered = details.stream()
                            .filter(od -> "DELIVERED".equals(od.getStatus()) || "SUCCESSFUL".equals(od.getStatus()))
                            .count();
                    
                    long cancelled = details.stream()
                            .filter(od -> "CANCELLED".equals(od.getStatus()))
                            .count();
                    
                    long problem = details.stream()
                            .filter(od -> "IN_TROUBLES".equals(od.getStatus()) || 
                                         "COMPENSATION".equals(od.getStatus()) || 
                                         "RETURNING".equals(od.getStatus()) || 
                                         "RETURNED".equals(od.getStatus()))
                            .count();
                    
                    return CustomerDashboardResponse.PackageStatusTrend.builder()
                            .date(dateLabel)
                            .inTransit(inTransit)
                            .delivered(delivered)
                            .cancelled(cancelled)
                            .problem(problem)
                            .total((long) details.size())
                            .build();
                })
                .collect(Collectors.toList());
    }
    
    private List<CustomerDashboardResponse.RecentIssue> buildRecentIssues(List<IssueEntity> issues) {
        return issues.stream()
                .filter(i -> i.getIssueTypeEntity() != null)
                .filter(i -> {
                    String typeName = i.getIssueTypeEntity().getIssueTypeName();
                    return !"OFFROUTE".equals(typeName) && !"TRAFFIC_PENALTY".equals(typeName);
                })
                .sorted((a, b) -> {
                    if (a.getReportedAt() == null) return 1;
                    if (b.getReportedAt() == null) return -1;
                    return b.getReportedAt().compareTo(a.getReportedAt());
                })
                .limit(10)
                .map(i -> {
                    String orderCode = "";
                    if (i.getOrderDetails() != null && !i.getOrderDetails().isEmpty()) {
                        OrderDetailEntity firstDetail = i.getOrderDetails().get(0);
                        if (firstDetail.getOrderEntity() != null) {
                            orderCode = firstDetail.getOrderEntity().getOrderCode();
                        }
                    }
                    return CustomerDashboardResponse.RecentIssue.builder()
                            .issueId(i.getId().toString())
                            .issueType(i.getIssueTypeEntity().getIssueTypeName())
                            .description(i.getDescription())
                            .status(i.getStatus())
                            .reportedAt(i.getReportedAt() != null ? i.getReportedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "")
                            .orderCode(orderCode)
                            .build();
                })
                .collect(Collectors.toList());
    }
    
    private List<CustomerDashboardResponse.TopRecipient> buildTopRecipients(List<OrderDetailEntity> orderDetails) {
        // Group by recipient from order
        Map<String, List<OrderDetailEntity>> byRecipient = orderDetails.stream()
                .filter(od -> od.getOrderEntity() != null)
                .filter(od -> od.getOrderEntity().getReceiverName() != null && !od.getOrderEntity().getReceiverName().isEmpty())
                .collect(Collectors.groupingBy(od -> {
                    String name = od.getOrderEntity().getReceiverName();
                    String phone = od.getOrderEntity().getReceiverPhone() != null 
                            ? od.getOrderEntity().getReceiverPhone() : "";
                    return name + "|" + phone;
                }));
        
        return byRecipient.entrySet().stream()
                .map(e -> {
                    String[] parts = e.getKey().split("\\|");
                    String name = parts[0];
                    String phone = parts.length > 1 ? parts[1] : "";
                    
                    List<OrderDetailEntity> details = e.getValue();
                    long total = details.size();
                    long successful = details.stream()
                            .filter(od -> "DELIVERED".equals(od.getStatus()) || "SUCCESSFUL".equals(od.getStatus()))
                            .count();
                    long failed = details.stream()
                            .filter(od -> "CANCELLED".equals(od.getStatus()) || "IN_TROUBLES".equals(od.getStatus()) ||
                                         "COMPENSATION".equals(od.getStatus()) || "RETURNING".equals(od.getStatus()) ||
                                         "RETURNED".equals(od.getStatus()))
                            .count();
                    
                    double successRate = total > 0 ? (double) successful / total * 100 : 0.0;
                    
                    // Get most common delivery address
                    String address = details.stream()
                            .filter(od -> od.getOrderEntity().getDeliveryAddress() != null)
                            .map(od -> od.getOrderEntity().getDeliveryAddress().getStreet())
                            .filter(Objects::nonNull)
                            .collect(Collectors.groupingBy(a -> a, Collectors.counting()))
                            .entrySet().stream()
                            .max(Map.Entry.comparingByValue())
                            .map(Map.Entry::getKey)
                            .orElse("");
                    
                    return CustomerDashboardResponse.TopRecipient.builder()
                            .recipientName(name)
                            .recipientPhone(phone)
                            .recipientAddress(address)
                            .totalPackages(total)
                            .successfulPackages(successful)
                            .failedPackages(failed)
                            .successRate(Math.round(successRate * 100.0) / 100.0)
                            .build();
                })
                .sorted(Comparator.comparingLong(CustomerDashboardResponse.TopRecipient::getTotalPackages).reversed())
                .limit(10)
                .collect(Collectors.toList());
    }

    private String generateCustomerAiSummary(
            CustomerDashboardResponse.OrderSummary orders,
            CustomerDashboardResponse.DeliveryPerformance delivery,
            CustomerDashboardResponse.FinancialSummary financial,
            CustomerDashboardResponse.ActionsSummary actions,
            DashboardFilterRequest filter) {

        String periodLabel = getPeriodLabel(filter);

        // Fallback summary (hiện đang dùng) phòng khi Gemini lỗi hoặc không cấu hình
        StringBuilder fallback = new StringBuilder();
        fallback.append(String.format("Trong %s, bạn có **%d đơn hàng** với **%d kiện hàng**. ",
                periodLabel, orders.getTotalOrders(), orders.getTotalOrderDetails()));

        if (orders.getDeliveredPackages() > 0) {
            fallback.append(String.format("**%d kiện** đã giao thành công (tỷ lệ: **%.1f%%**). ",
                    orders.getDeliveredPackages(), orders.getSuccessRate()));
        }

        if (orders.getInTransitPackages() > 0) {
            fallback.append(String.format("**%d kiện** đang vận chuyển. ", orders.getInTransitPackages()));
        }

        if (actions.getContractsToSign() > 0) {
            fallback.append(String.format("Bạn có **%d hợp đồng** cần ký. ", actions.getContractsToSign()));
        }

        if (actions.getPaymentsNeeded() > 0) {
            fallback.append(String.format("Có **%d thanh toán** đang chờ. ", actions.getPaymentsNeeded()));
        }

        if (delivery.getIssueCount() > 0) {
            fallback.append(String.format("Có **%d sự cố** liên quan đến đơn hàng của bạn. ", delivery.getIssueCount()));
        }

        // Gọi Gemini để tạo tóm tắt giàu ngữ cảnh hơn
        try {
            String systemPrompt = "Bạn là trợ lý phân tích cho khách hàng của hệ thống quản lý vận tải. " +
                    "Hãy tóm tắt ngắn gọn (3-5 câu) tình hình vận chuyển trong " + periodLabel +
                    " bằng tiếng Việt, giọng thân thiện, súc tích. " +
                    "Phải phân tích TẤT CẢ các số liệu sau: tổng đơn hàng, kiện hàng, tỷ lệ thành công, kiện đang vận chuyển, " +
                    "tài chính (đã thanh toán/chờ thanh toán), hành động cần làm (hợp đồng, thanh toán), VÀ QUAN TRỌNG NHẤT: " +
                    "sự cố và các vấn đề. " +
                    "Nếu có bất kỳ sự cố nào (cancelled > 0, problem > 0, hoặc issueCount > 0), hãy nhấn mạnh và đề cập rõ ràng. " +
                    "Nếu không có sự cố nào, có thể nói 'hoạt động tốt'. " +
                    "QUAN TRỌNG: Sử dụng định dạng **in đậm** cho tất cả các số liệu quan trọng, tỷ lệ phần trăm, và những điểm cần chú ý đặc biệt.";

            StringBuilder userContext = new StringBuilder();
            userContext.append("=== PHÂN TÍCH DỮ LIỆU DASHBOARD ===\n");
            userContext.append(String.format("• Tổng đơn hàng: %d\n", orders.getTotalOrders()));
            userContext.append(String.format("• Tổng kiện hàng: %d\n", orders.getTotalOrderDetails()));
            userContext.append(String.format("• Kiện đã giao thành công: %d (%.1f%%)\n", orders.getDeliveredPackages(), orders.getSuccessRate()));
            userContext.append(String.format("• Kiện đang vận chuyển: %d\n", orders.getInTransitPackages()));
            userContext.append(String.format("• Kiện bị hủy: %d\n", orders.getCancelledPackages()));
            userContext.append(String.format("• Kiện gặp vấn đề: %d\n", orders.getProblemPackages()));
            userContext.append(String.format("• Tỷ lệ thành công: %.1f%%\n", orders.getSuccessRate()));
            userContext.append("=== TÀI CHÍNH ===\n");
            userContext.append(String.format("• Đã thanh toán: %s VNĐ\n", formatCurrency(financial.getTotalPaid())));
            userContext.append(String.format("• Chờ thanh toán: %s VNĐ\n", formatCurrency(financial.getPendingPayment())));
            userContext.append("=== HÀNH ĐỘNG CẦN LÀM ===\n");
            userContext.append(String.format("• Hợp đồng cần ký: %d\n", actions.getContractsToSign()));
            userContext.append(String.format("• Thanh toán cần thực hiện: %d\n", actions.getPaymentsNeeded()));
            userContext.append("=== SỰ CỐ VÀ VẤN ĐỀ ===\n");
            userContext.append(String.format("• Tổng số sự cố: %d\n", delivery.getIssueCount()));
            userContext.append(String.format("• Tỷ lệ sự cố: %.1f%%\n", delivery.getIssueRate()));
            
            // Log the exact context being sent to AI for debugging
            log.info("[Customer AI Summary] Sending to AI:\n{}", userContext.toString());

            List<ChatMessage> messages = List.of(
                    new ChatMessage("user", userContext.toString())
            );

            String geminiSummary = geminiService.generateResponse(systemPrompt, messages);
            if (geminiSummary != null && !geminiSummary.isBlank()) {
                log.info("[Customer AI Summary] AI Response: {}", geminiSummary);
                return geminiSummary;
            }
        } catch (Exception e) {
            log.error("[CustomerDashboard] Gemini summary failed, using fallback summary", e);
        }

        return fallback.toString();
    }

    // ==================== Driver Dashboard Helpers ====================
    
    private List<DriverDashboardResponse.TripTrendPoint> buildTripTrendData(
            List<VehicleAssignmentEntity> assignments, 
            DashboardFilterRequest filter) {
        
        // Generate full set of date labels for range (WEEK/MONTH/YEAR/CUSTOM)
        List<String> allDateLabels = generateAllDateLabels(filter);

        // Group assignments by normalized date key
        Map<String, List<VehicleAssignmentEntity>> groupedByPeriod = assignments.stream()
                .filter(a -> a.getCreatedAt() != null)
                .collect(Collectors.groupingBy(
                        a -> getDateKey(a.getCreatedAt(), filter),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
        
        // Build trend points - ensure all dates exist, even if 0 trips
        return allDateLabels.stream()
                .map(label -> {
                    List<VehicleAssignmentEntity> periodAssignments = groupedByPeriod
                            .getOrDefault(label, Collections.emptyList());
                    
                    int tripsCompleted = (int) periodAssignments.stream()
                            .filter(a -> "COMPLETED".equals(a.getStatus()))
                            .count();
                    
                    return DriverDashboardResponse.TripTrendPoint.builder()
                            .label(label)
                            .tripsCompleted(tripsCompleted)
                            .build();
                })
                .collect(Collectors.toList());
    }
    
    private String getPeriodLabel(LocalDateTime dateTime, DashboardFilterRequest filter) {
        return switch (filter.getRange()) {
            case WEEK -> dateTime.toLocalDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")); // Daily for week with full date
            case MONTH -> dateTime.format(DateTimeFormatter.ofPattern("dd/MM")); // Daily for month (same month/year)
            case YEAR -> dateTime.format(DateTimeFormatter.ofPattern("MM/yyyy")); // Monthly for year
            case CUSTOM -> dateTime.format(DateTimeFormatter.ofPattern("dd/MM")); // Daily for custom (adjusted in getDateKey)
        };
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0";
        return String.format("%,.0f", amount);
    }
    
    private String getPeriodLabel(DashboardFilterRequest filter) {
        return switch (filter.getRange()) {
            case WEEK -> "7 ngày qua";
            case MONTH -> "tháng này";
            case YEAR -> "năm nay";
            case CUSTOM -> "khoảng thời gian";
        };
    }
    
    /**
     * Generate all date labels for a time range to ensure line charts show all points
     * @param filter Dashboard filter with time range
     * @return List of date labels in order
     */
    private List<String> generateAllDateLabels(DashboardFilterRequest filter) {
        List<String> labels = new ArrayList<>();
        LocalDateTime start = filter.getStartDate();
        LocalDateTime end = filter.getEndDate();
        
        switch (filter.getRange()) {
            case WEEK -> {
                // Generate daily labels for 7 days (full date yyyy-MM-dd)
                LocalDateTime current = start;
                while (!current.isAfter(end)) {
                    labels.add(current.toLocalDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                    current = current.plusDays(1);
                }
            }
            case MONTH -> {
                // Generate daily labels for the month
                LocalDateTime current = start;
                while (!current.isAfter(end)) {
                    labels.add(current.format(DateTimeFormatter.ofPattern("dd/MM")));
                    current = current.plusDays(1);
                }
            }
            case YEAR -> {
                // Generate monthly labels for the year
                LocalDateTime current = start.withDayOfMonth(1);
                while (!current.isAfter(end)) {
                    labels.add(current.format(DateTimeFormatter.ofPattern("MM/yyyy")));
                    current = current.plusMonths(1);
                }
            }
            case CUSTOM -> {
                // For custom range, determine granularity based on duration
                long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(start, end);
                if (daysBetween <= 31) {
                    // Daily labels
                    LocalDateTime current = start;
                    while (!current.isAfter(end)) {
                        labels.add(current.format(DateTimeFormatter.ofPattern("dd/MM")));
                        current = current.plusDays(1);
                    }
                } else {
                    // Monthly labels
                    LocalDateTime current = start.withDayOfMonth(1);
                    while (!current.isAfter(end)) {
                        labels.add(current.format(DateTimeFormatter.ofPattern("MM/yyyy")));
                        current = current.plusMonths(1);
                    }
                }
            }
        }
        return labels;
    }
    
    /**
     * Get the date format pattern based on time range
     */
    private String getDateFormatPattern(DashboardFilterRequest filter) {
        return switch (filter.getRange()) {
            case WEEK -> "yyyy-MM-dd";
            case MONTH, CUSTOM -> "dd/MM";
            case YEAR -> "MM/yyyy";
        };
    }
    
    /**
     * Get the date key from a LocalDateTime based on filter range
     */
    private String getDateKey(LocalDateTime dateTime, DashboardFilterRequest filter) {
        if (dateTime == null) return null;
        return switch (filter.getRange()) {
            case WEEK -> dateTime.toLocalDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")); // Full date key
            case MONTH -> dateTime.format(DateTimeFormatter.ofPattern("dd/MM")); // Same month, no year needed
            case YEAR -> dateTime.format(DateTimeFormatter.ofPattern("MM/yyyy"));
            case CUSTOM -> {
                long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(filter.getStartDate(), filter.getEndDate());
                // For custom range, check if spans multiple years
                boolean spansMultipleYears = filter.getStartDate().getYear() != filter.getEndDate().getYear();
                if (daysBetween <= 31) {
                    // Use full date if spans years, otherwise dd/MM is enough
                    yield spansMultipleYears 
                        ? dateTime.toLocalDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                        : dateTime.format(DateTimeFormatter.ofPattern("dd/MM"));
                } else {
                    yield dateTime.format(DateTimeFormatter.ofPattern("MM/yyyy"));
                }
            }
        };
    }
    
    /**
     * Count incidents for driver within date range (optimized)
     */
    private int getIncidentsCountForDriver(UUID driverId, LocalDateTime startDate, LocalDateTime endDate) {
        Long count = issueRepository.countByDriverIdAndReportedAtBetween(driverId, startDate, endDate);
        return count != null ? count.intValue() : 0;
    }
    
    /**
     * Count traffic violations for driver within date range
     */
    private int getTrafficViolationsCountForDriver(UUID driverId, LocalDateTime startDate, LocalDateTime endDate) {
        // Get all penalty histories for driver
        List<PenaltyHistoryEntity> penalties = penaltyHistoryRepository.findByDriverId(driverId);
        
        // Filter by date range
        return (int) penalties.stream()
                .filter(p -> p.getPenaltyDate() != null)
                .filter(p -> {
                    LocalDateTime penaltyDateTime = p.getPenaltyDate().atStartOfDay();
                    return !penaltyDateTime.isBefore(startDate) && !penaltyDateTime.isAfter(endDate);
                })
                .count();
    }
    
    /**
     * Build recent orders list (latest 5 order details for driver)
     */
    private List<DriverDashboardResponse.RecentOrder> buildRecentOrders(List<VehicleAssignmentEntity> allDriverAssignments) {
        // Get all order details from driver's assignments using service
        List<OrderDetailEntity> allOrderDetails = new ArrayList<>();
        for (VehicleAssignmentEntity assignment : allDriverAssignments) {
            List<OrderDetailEntity> assignmentOrderDetails = orderDetailEntityService.findByVehicleAssignmentEntity(assignment);
            allOrderDetails.addAll(assignmentOrderDetails);
        }
        
        // Deduplicate by Order ID - keep the most recent order detail for each order
        Map<UUID, OrderDetailEntity> mostRecentOrderDetails = new HashMap<>();
        for (OrderDetailEntity orderDetail : allOrderDetails) {
            OrderEntity order = orderDetail.getOrderEntity();
            if (order != null) {
                UUID orderId = order.getId();
                OrderDetailEntity existing = mostRecentOrderDetails.get(orderId);
                
                // Keep the most recent order detail for this order
                if (existing == null || 
                    (orderDetail.getCreatedAt() != null && existing.getCreatedAt() != null && 
                     orderDetail.getCreatedAt().isAfter(existing.getCreatedAt()))) {
                    mostRecentOrderDetails.put(orderId, orderDetail);
                }
            }
        }
        
        // Sort deduplicated order details by creation date and limit to 5
        return mostRecentOrderDetails.values().stream()
                .sorted((a, b) -> {
                    if (a.getCreatedAt() == null) return 1;
                    if (b.getCreatedAt() == null) return -1;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .limit(5)
                .map(orderDetail -> {
                    OrderEntity order = orderDetail.getOrderEntity();
                    String orderCode = order != null ? order.getOrderCode() : "";
                    String receiverName = order != null ? order.getReceiverName() : "";
                    String receiverPhone = order != null ? order.getReceiverPhone() : "";
                    String orderStatus = order != null ? order.getStatus() : orderDetail.getStatus();
                    
                    return DriverDashboardResponse.RecentOrder.builder()
                            .orderId(order != null ? order.getId().toString() : "")
                            .orderCode(orderCode)
                            .status(orderStatus)
                            .receiverName(receiverName)
                            .receiverPhone(receiverPhone)
                            .createdDate(orderDetail.getCreatedAt() != null ? 
                                    orderDetail.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "")
                            .trackingCode(orderDetail.getTrackingCode())
                            .build();
                })
                .collect(Collectors.toList());
    }
    
    // ==================== Admin Dashboard Additional Helpers ====================
    
    private List<AdminDashboardResponse.TopPerformer> buildTopStaff(List<IssueEntity> issues) {
        // Group issues by staff and count resolved issues
        Map<UUID, Long> resolvedByStaff = issues.stream()
                .filter(i -> i.getStaff() != null && "RESOLVED".equals(i.getStatus()))
                .collect(Collectors.groupingBy(
                        i -> i.getStaff().getId(),
                        Collectors.counting()
                ));
        
        // Build top staff list
        return resolvedByStaff.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(5)
                .map(entry -> {
                    try {
                        capstone_project.entity.auth.UserEntity staff = userRepository.findById(entry.getKey()).orElse(null);
                        if (staff != null) {
                            return AdminDashboardResponse.TopPerformer.builder()
                                    .id(staff.getId().toString())
                                    .name(staff.getFullName())
                                    .orderCount(entry.getValue())
                                    .rank(0) // Will be set after sorting
                                    .build();
                        }
                    } catch (Exception e) {
                        log.warn("Failed to get staff info: {}", e.getMessage());
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
    
    private AdminDashboardResponse.RegistrationData buildRegistrationData(
            LocalDateTime startDate, LocalDateTime endDate, DashboardFilterRequest filter) {
        
        // Get customer registrations
        List<capstone_project.entity.auth.UserEntity> customers = userRepository.findByRoleAndCreatedAtBetween("CUSTOMER", startDate, endDate);
        List<AdminDashboardResponse.TrendDataPoint> customerRegistrations = buildUserRegistrationTrend(customers, startDate, endDate, filter);
        
        // Get staff registrations
        List<capstone_project.entity.auth.UserEntity> staff = userRepository.findByRoleAndCreatedAtBetween("STAFF", startDate, endDate);
        List<AdminDashboardResponse.TrendDataPoint> staffRegistrations = buildUserRegistrationTrend(staff, startDate, endDate, filter);
        
        // Get driver registrations
        List<capstone_project.entity.auth.UserEntity> drivers = userRepository.findByRoleAndCreatedAtBetween("DRIVER", startDate, endDate);
        List<AdminDashboardResponse.TrendDataPoint> driverRegistrations = buildUserRegistrationTrend(drivers, startDate, endDate, filter);
        
        return AdminDashboardResponse.RegistrationData.builder()
                .customerRegistrations(customerRegistrations)
                .staffRegistrations(staffRegistrations)
                .driverRegistrations(driverRegistrations)
                .build();
    }
    
    private List<AdminDashboardResponse.TrendDataPoint> buildUserRegistrationTrend(
            List<capstone_project.entity.auth.UserEntity> users, 
            LocalDateTime startDate, 
            LocalDateTime endDate, 
            DashboardFilterRequest filter) {
        
        // Group users by period
        Map<String, List<capstone_project.entity.auth.UserEntity>> groupedByPeriod = users.stream()
                .collect(Collectors.groupingBy(u -> getPeriodLabel(u.getCreatedAt(), filter)));
        
        // Build trend points
        return groupedByPeriod.entrySet().stream()
                .map(entry -> AdminDashboardResponse.TrendDataPoint.builder()
                        .label(entry.getKey())
                        .count(entry.getValue().size())
                        .amount(BigDecimal.ZERO)
                        .build())
                .sorted((a, b) -> a.getLabel().compareTo(b.getLabel()))
                .collect(Collectors.toList());
    }
    
    private String buildFallbackAdminSummary(
            AdminDashboardResponse.KpiSummary kpi,
            AdminDashboardResponse.DeliveryPerformance delivery,
            AdminDashboardResponse.IssueRefundSummary issues,
            AdminDashboardResponse.FleetHealthSummary fleet,
            DashboardFilterRequest filter) {
        
        String periodLabel = getPeriodLabel(filter);
        StringBuilder fallback = new StringBuilder();
        
        fallback.append(String.format("Trong %s, hệ thống có **%d đơn hàng** với **%d kiện hàng**, ", 
                periodLabel, kpi.getTotalOrders(), kpi.getTotalOrderDetails()));
        fallback.append(String.format("doanh thu đạt **%s VNĐ**. ", formatCurrency(kpi.getTotalRevenue())));
        fallback.append(String.format("Tỷ lệ giao hàng đúng hẹn: **%.1f%%**. ", delivery.getOnTimePercentage()));
        
        if (issues.getOpenIssues() > 0) {
            fallback.append(String.format("Có **%d sự cố** đang mở cần xử lý. ", issues.getOpenIssues()));
        }
        
        if (fleet.getOverdueMaintenanceVehicles() > 0) {
            fallback.append(String.format("**%d xe** đã quá hạn bảo dưỡng. ", fleet.getOverdueMaintenanceVehicles()));
        }
        
        return fallback.toString();
    }
}
