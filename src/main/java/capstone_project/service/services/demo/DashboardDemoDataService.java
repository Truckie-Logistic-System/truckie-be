package capstone_project.service.services.demo;

import capstone_project.common.enums.IssueCategoryEnum;
import capstone_project.common.enums.VehicleStatusEnum;
import capstone_project.dtos.request.demo.GenerateDemoDataRequest;
import capstone_project.dtos.response.demo.DemoDataSummary;
import capstone_project.entity.auth.UserEntity;
import capstone_project.entity.issue.IssueEntity;
import capstone_project.entity.issue.IssueTypeEntity;
import capstone_project.entity.order.contract.ContractEntity;
import capstone_project.entity.order.order.OrderDetailEntity;
import capstone_project.entity.order.order.OrderEntity;
import capstone_project.entity.order.order.RefundEntity;
import capstone_project.entity.order.transaction.TransactionEntity;
import capstone_project.entity.user.customer.CustomerEntity;
import capstone_project.entity.user.driver.DriverEntity;
import capstone_project.entity.user.driver.PenaltyHistoryEntity;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.entity.vehicle.VehicleEntity;
import capstone_project.entity.order.order.VehicleFuelConsumptionEntity;
import capstone_project.entity.vehicle.VehicleMaintenanceEntity;
import capstone_project.entity.auth.RoleEntity;
import capstone_project.repository.entityServices.issue.IssueEntityService;
import capstone_project.repository.entityServices.issue.IssueTypeEntityService;
import capstone_project.repository.entityServices.order.VehicleFuelConsumptionEntityService;
import capstone_project.repository.entityServices.order.contract.ContractEntityService;
import capstone_project.repository.entityServices.order.order.OrderDetailEntityService;
import capstone_project.repository.entityServices.order.order.OrderEntityService;
import capstone_project.repository.entityServices.order.transaction.TransactionEntityService;
import capstone_project.repository.entityServices.refund.RefundEntityService;
import capstone_project.repository.entityServices.user.CustomerEntityService;
import capstone_project.repository.entityServices.user.DriverEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleAssignmentEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleMaintenanceEntityService;
import capstone_project.repository.repositories.auth.RoleRepository;
import capstone_project.repository.repositories.auth.UserRepository;
import capstone_project.repository.repositories.issue.IssueRepository;
import capstone_project.repository.repositories.order.VehicleFuelConsumptionRepository;
import capstone_project.repository.repositories.order.contract.ContractRepository;
import capstone_project.repository.repositories.order.order.OrderDetailRepository;
import capstone_project.repository.repositories.order.order.OrderRepository;
import capstone_project.repository.repositories.refund.RefundRepository;
import capstone_project.repository.repositories.order.transaction.TransactionRepository;
import capstone_project.repository.repositories.user.CustomerRepository;
import capstone_project.repository.repositories.user.DriverRepository;
import capstone_project.repository.repositories.user.PenaltyHistoryRepository;
import capstone_project.repository.repositories.vehicle.VehicleAssignmentRepository;
import capstone_project.repository.repositories.vehicle.VehicleMaintenanceRepository;
import capstone_project.repository.repositories.vehicle.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Service for generating demo/test data for dashboard visualization
 * Implements high-season distribution pattern (Q3 peak)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardDemoDataService {

    @PersistenceContext
    private EntityManager entityManager;
    private final DriverEntityService driverEntityService;
    private final OrderEntityService orderEntityService;
    private final OrderDetailEntityService orderDetailEntityService;
    private final VehicleAssignmentEntityService vehicleAssignmentEntityService;
    private final ContractEntityService contractEntityService;
    private final TransactionEntityService transactionEntityService;
    private final RefundEntityService refundEntityService;
    private final IssueEntityService issueEntityService;
    private final IssueTypeEntityService issueTypeEntityService;
    private final VehicleEntityService vehicleEntityService;
    private final VehicleMaintenanceEntityService vehicleMaintenanceEntityService;
    private final VehicleFuelConsumptionEntityService vehicleFuelConsumptionEntityService;
    private final CustomerEntityService customerEntityService;
    
    // Repositories (for delete operations)
    private final VehicleFuelConsumptionRepository vehicleFuelConsumptionRepository;
    private final PenaltyHistoryRepository penaltyHistoryRepository;
    private final VehicleMaintenanceRepository vehicleMaintenanceRepository;
    private final RefundRepository refundRepository;
    private final TransactionRepository transactionRepository;
    private final IssueRepository issueRepository;
    private final ContractRepository contractRepository;
    private final VehicleAssignmentRepository vehicleAssignmentRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final OrderRepository orderRepository;
    private final VehicleRepository vehicleRepository;
    private final CustomerRepository customerRepository;
    private final DriverRepository driverRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    // Seasonal multipliers for realistic distribution
    private static final Map<Integer, Double> SEASONAL_MULTIPLIERS = Map.ofEntries(
            Map.entry(1, 0.6),  // Jan - Low (post-holiday)
            Map.entry(2, 0.7),  // Feb - Low
            Map.entry(3, 1.0),  // Mar - Normal
            Map.entry(4, 1.1),  // Apr - Rising
            Map.entry(5, 1.2),  // May - Rising
            Map.entry(6, 1.5),  // Jun - High season start
            Map.entry(7, 1.8),  // Jul - Peak
            Map.entry(8, 2.0),  // Aug - Peak (Q3 highest)
            Map.entry(9, 1.7),  // Sep - High season end
            Map.entry(10, 1.0), // Oct - Normal
            Map.entry(11, 0.9), // Nov - Cooling
            Map.entry(12, 1.3)  // Dec - Year-end mini peak
    );

    private static final String[] DEMO_CUSTOMER_NAMES = {
            "Công ty TNHH Vận Tải Alpha", "Công ty CP Logistics Beta", "Doanh nghiệp Thương Mại Gamma",
            "Công ty TNHH XNK Delta", "Công ty CP Sản Xuất Epsilon", "Xí Nghiệp Vận Chuyển Zeta",
            "Công ty TNHH Phân Phối Eta", "Doanh nghiệp Tư Nhân Theta", "Công ty CP Thực Phẩm Iota",
            "Công ty TNHH Điện Tử Kappa", "Tập đoàn Vận Tải Lambda", "Công ty Logistics Mu",
            "Doanh nghiệp Xuất Nhập Khẩu Nu", "Công ty CP Kho Vận Xi", "Xí nghiệp Đường Bộ Omicron",
            "Công ty TNHH Giao Thông Pi", "Tập đoàn Logistics Rho", "Công ty CP Vận Tải Sigma",
            "Doanh nghiệp Thương Mại Tau", "Công ty TNHH Phân Phối Upsilon", "Xí nghiệp Vận Chuyển Phi",
            "Công ty CP Kho Vận Chi", "Tập đoàn Giao Thông Psi", "Doanh nghiệp Logistics Omega"
    };

    private static final String[] DEMO_DRIVER_NAMES = {
            "Nguyễn Văn An", "Trần Văn Bình", "Lê Văn Cường", "Phạm Văn Dũng", "Hoàng Văn Em",
            "Đặng Văn Phúc", "Vũ Văn Giang", "Bùi Văn Hải", "Đỗ Văn Kiên", "Ngô Văn Long",
            "Phan Văn Minh", "Lương Văn Nam", "Mai Văn Phong", "Trương Văn Quang", "Đinh Văn Sơn",
            "Huỳnh Văn Tâm", "Võ Văn Tuấn", "Dương Văn Việt", "Lý Văn Xuân", "Hồ Văn Yên"
    };

    private static final String[] DEMO_STAFF_NAMES = {
            "Nguyễn Thị Hoa", "Trần Thị Lan", "Lê Thị Mai", "Phạm Thị Nga", "Hoàng Thị Oanh"
    };

    private static final String[] RECEIVER_NAMES = {
            "Nguyễn Văn A", "Trần Thị B", "Lê Văn C", "Phạm Thị D", "Hoàng Văn E",
            "Đặng Thị F", "Vũ Văn G", "Bùi Thị H", "Đỗ Văn I", "Ngô Thị K"
    };

    private static final String[] CITIES = {
            "Hà Nội", "Hồ Chí Minh", "Đà Nẵng", "Hải Phòng", "Cần Thơ",
            "Biên Hòa", "Nha Trang", "Huế", "Vũng Tàu", "Quy Nhơn"
    };

    private static final String[] ORDER_DETAIL_STATUSES = {
            "PENDING", "ON_PLANNING", "ASSIGNED_TO_DRIVER", "PICKING_UP",
            "ON_DELIVERED", "ONGOING_DELIVERED", "DELIVERED", "SUCCESSFUL",
            "IN_TROUBLES", "COMPENSATION", "RETURNING", "RETURNED", "CANCELLED"
    };

    private static final String[] VEHICLE_TYPES = {
            "TRUCK_1_TON", "TRUCK_2_TON", "TRUCK_3_5_TON", "TRUCK_5_TON",
            "TRUCK_8_TON", "TRUCK_10_TON", "TRUCK_15_TON", "TRUCK_20_TON"
    };

    /**
     * Generate demo data for all dashboards
     */
    @Transactional
    public DemoDataSummary generateDemoData(GenerateDemoDataRequest request) {
        long startTime = System.currentTimeMillis();
        log.info("🎯 Starting demo data generation for year {}", request.getYear());

        DemoDataSummary summary = DemoDataSummary.builder()
                .year(request.getYear())
                .build();

        try {
            // Reset global counters for even distribution
            globalStaffCounter = 0;
            globalDriverCounter = 0;
            
            // Step 1: Create demo users (customers, staff, drivers) - distributed across months
            Map<String, List<?>> demoUsers = createDemoUsers(request);
            summary.setUsersCreated(((List<?>) demoUsers.get("users")).size());
            summary.setCustomersCreated(((List<?>) demoUsers.get("customers")).size());
            summary.setStaffCreated(((List<?>) demoUsers.get("staff")).size());
            summary.setDriversCreated(((List<?>) demoUsers.get("drivers")).size());
            
            List<UserEntity> staffUsers = (List<UserEntity>) demoUsers.get("staff");

            // Step 2: Create demo vehicles
            List<VehicleEntity> demoVehicles = createDemoVehicles(request);
            summary.setVehiclesCreated(demoVehicles.size());

            // Step 3: Generate orders, trips, issues month by month
            Map<String, Integer> entityCounts = generateMonthlyData(
                    request,
                    (List<CustomerEntity>) demoUsers.get("customers"),
                    (List<DriverEntity>) demoUsers.get("drivers"),
                    staffUsers,
                    demoVehicles
            );

            summary.setOrdersCreated(entityCounts.get("orders"));
            summary.setOrderDetailsCreated(entityCounts.get("orderDetails"));
            summary.setVehicleAssignmentsCreated(entityCounts.get("assignments"));
            summary.setContractsCreated(entityCounts.get("contracts"));
            summary.setTransactionsCreated(entityCounts.get("transactions"));
            summary.setRefundsCreated(entityCounts.get("refunds"));
            summary.setIssuesCreated(entityCounts.get("issues"));
            summary.setMaintenancesCreated(entityCounts.get("maintenances"));
            summary.setPenaltiesCreated(entityCounts.get("penalties"));
            summary.setFuelConsumptionsCreated(entityCounts.get("fuelConsumptions"));

            long duration = System.currentTimeMillis() - startTime;
            summary.setExecutionTimeMs(duration);
            summary.setMessage("Demo data generated successfully for year " + request.getYear());

            log.info("✅ Demo data generation completed in {}ms", duration);
            return summary;

        } catch (Exception e) {
            log.error("❌ Error generating demo data", e);
            throw new RuntimeException("Failed to generate demo data: " + e.getMessage(), e);
        }
    }

    /**
     * Clear all demo data marked with isDemoData = true - ABSOLUTE DELETION
     * Uses PostgreSQL session_replication_role to bypass FK constraints temporarily
     * This is the simplest and most reliable approach for demo data cleanup
     */
    @Transactional
    public DemoDataSummary clearDemoData() {
        long startTime = System.currentTimeMillis();
        log.info("🧹 Starting ABSOLUTE demo data cleanup - bypassing FK constraints temporarily");

        try {
            // STEP 0: DISABLE FOREIGN KEY CONSTRAINTS TEMPORARILY
            log.info("� STEP 0: Disabling foreign key constraints...");
            entityManager.createNativeQuery("SET session_replication_role = replica").executeUpdate();
            log.info("✅ FK constraints disabled successfully");

            int totalDeleted = 0;

            // STEP 1: DELETE ALL DEMO DATA (order doesn't matter now)
            log.info("🗑️ STEP 1: Deleting all demo data...");
            
            // Delete all entities with isDemoData = true
            totalDeleted += deleteDemoFuelConsumptions();
            totalDeleted += deleteDemoPenalties();
            totalDeleted += deleteDemoMaintenances();
            totalDeleted += deleteDemoRefunds();
            totalDeleted += deleteDemoTransactions();
            totalDeleted += deleteDemoIssues();
            totalDeleted += deleteDemoVehicleAssignments();
            totalDeleted += deleteDemoOrderDetails();
            totalDeleted += deleteDemoContracts();
            totalDeleted += deleteDemoOrders();
            totalDeleted += deleteDemoVehicles();
            totalDeleted += deleteDemoCustomers();
            totalDeleted += deleteDemoDrivers();
            totalDeleted += deleteDemoUsers();

            // STEP 2: RE-ENABLE FOREIGN KEY CONSTRAINTS
            log.info("🔒 STEP 2: Re-enabling foreign key constraints...");
            entityManager.createNativeQuery("SET session_replication_role = DEFAULT").executeUpdate();
            log.info("✅ FK constraints re-enabled successfully");

            long duration = System.currentTimeMillis() - startTime;
            
            return DemoDataSummary.builder()
                    .totalRecordsDeleted(totalDeleted)
                    .executionTimeMs(duration)
                    .message("✅ ABSOLUTE DELETION: Successfully cleared " + totalDeleted + " demo records (FK bypassed)")
                    .build();

        } catch (Exception e) {
            // Make sure to re-enable FK constraints even if deletion fails
            try {
                log.warn("⚠️ Attempting to re-enable FK constraints after error...");
                entityManager.createNativeQuery("SET session_replication_role = DEFAULT").executeUpdate();
                log.info("✅ FK constraints re-enabled after error");
            } catch (Exception restoreException) {
                log.error("❌ CRITICAL: Failed to re-enable FK constraints after error!", restoreException);
            }
            
            log.error("❌ Error in absolute demo data cleanup", e);
            throw new RuntimeException("Failed to clear demo data: " + e.getMessage(), e);
        }
    }

    /**
     * Create demo users (customers, staff, drivers) distributed across the year
     */
    private Map<String, List<?>> createDemoUsers(GenerateDemoDataRequest request) {
        List<UserEntity> allUsers = new ArrayList<>();
        List<CustomerEntity> customers = new ArrayList<>();
        List<UserEntity> staff = new ArrayList<>(); // Staff are just users without separate entity
        List<DriverEntity> drivers = new ArrayList<>();

        int year = request.getYear();
        
        // If targetCustomerId is specified, fetch and add to list
        if (request.getTargetCustomerId() != null) {
            customerEntityService.findEntityById(request.getTargetCustomerId())
                    .ifPresent(customer -> {
                        customers.add(customer);
                        log.info("Added target customer: {} ({})", customer.getCompanyName(), customer.getId());
                    });
        }
        
        // If targetDriverId is specified, fetch and add to list
        if (request.getTargetDriverId() != null) {
            driverEntityService.findEntityById(request.getTargetDriverId())
                    .ifPresent(driver -> {
                        drivers.add(driver);
                        log.info("Added target driver: {} ({})", driver.getUser().getFullName(), driver.getId());
                    });
        }

        // Create customers
        if (request.getInclude().getCustomer()) {
            for (int i = 0; i < DEMO_CUSTOMER_NAMES.length; i++) {
                int month = (i % 12) + 1;
                LocalDateTime createdAt = randomDateTimeInMonth(year, month);

                UserEntity user = createDemoUser("customer" + i + "@demo.com", createdAt, "CUSTOMER");
                allUsers.add(user);

                CustomerEntity customer = CustomerEntity.builder()
                        .user(user)
                        .companyName(DEMO_CUSTOMER_NAMES[i])
                        .businessAddress(randomAddress())
                        .representativeName("Rep " + DEMO_CUSTOMER_NAMES[i])
                        .representativePhone("09" + String.format("%08d", 10000000 + i))
                        .status("ACTIVE")
                        .isDemoData(true)
                        .build();
        // IMPORTANT: Set isDemoData FIRST, then createdAt to prevent override
        customer.setIsDemoData(true);
        customer.setCreatedAt(createdAt);
                customers.add(customerEntityService.save(customer));
            }
        }

        // Create staff (just users with STAFF role, no separate entity)
        if (request.getInclude().getStaff()) {
            for (int i = 0; i < DEMO_STAFF_NAMES.length; i++) {
                int month = (i % 12) + 1;
                LocalDateTime createdAt = randomDateTimeInMonth(year, month);

                UserEntity user = createDemoUser("staff" + i + "@demo.com", createdAt, "STAFF");
                allUsers.add(user);
                staff.add(user);
            }
        }

        // Create drivers
        if (request.getInclude().getDriver()) {
            for (int i = 0; i < DEMO_DRIVER_NAMES.length; i++) {
                int month = (i % 12) + 1;
                LocalDateTime createdAt = randomDateTimeInMonth(year, month);

                UserEntity user = createDemoUser("driver" + i + "@demo.com", createdAt, "DRIVER");
                allUsers.add(user);

                DriverEntity driver = DriverEntity.builder()
                        .user(user)
                        .driverLicenseNumber("LICENSE-DEMO-" + String.format("%03d", i))
                        .identityNumber("IDENTITY-" + String.format("%09d", 300000000 + i))
                        .status("ACTIVE")
                        .isDemoData(true)
                        .build();
        // IMPORTANT: Set isDemoData FIRST, then createdAt to prevent override
        driver.setIsDemoData(true);
        driver.setCreatedAt(createdAt);
                drivers.add(driverEntityService.save(driver));
            }
        }

        log.info("Created {} demo users: {} customers, {} staff, {} drivers",
                allUsers.size(), customers.size(), staff.size(), drivers.size());

        Map<String, List<?>> result = new HashMap<>();
        result.put("users", allUsers);
        result.put("customers", customers);
        result.put("staff", staff);
        result.put("drivers", drivers);
        return result;
    }

    private UserEntity createDemoUser(String email, LocalDateTime createdAt, String roleName) {
        // Fetch role entity by name
        RoleEntity role = roleRepository.findAll().stream()
                .filter(r -> roleName.equals(r.getRoleName()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));
        
        UserEntity user = UserEntity.builder()
                .email(email)
                .username(email)
                .fullName("Demo " + roleName + " User")
                .phoneNumber("0900000000")
                .password("$2a$10$demo.hashed.password") // Pre-hashed demo password
                .role(role)
                .status("ACTIVE")
                .isDemoData(true)
                .build();
        // IMPORTANT: Set isDemoData FIRST, then createdAt to prevent override
        user.setIsDemoData(true);
        user.setCreatedAt(createdAt);
        return userRepository.save(user);
    }

    /**
     * Create demo vehicles distributed across the year
     */
    private List<VehicleEntity> createDemoVehicles(GenerateDemoDataRequest request) {
        List<VehicleEntity> vehicles = new ArrayList<>();

        for (int i = 0; i < 30; i++) {
            String vehicleType = VEHICLE_TYPES[i % VEHICLE_TYPES.length];
            
            // Distribute vehicle creation across the year for realistic dashboard data
            LocalDateTime vehicleCreatedAt = randomDateTimeInYear(request.getYear());
            
            // Distribute vehicle status evenly: ACTIVE, IN_TRANSIT, MAINTENANCE
            VehicleStatusEnum[] statuses = {VehicleStatusEnum.ACTIVE, VehicleStatusEnum.IN_TRANSIT, VehicleStatusEnum.MAINTENANCE};
            VehicleStatusEnum vehicleStatus = statuses[i % statuses.length];
            
            VehicleEntity vehicle = VehicleEntity.builder()
                    .licensePlateNumber("DEMO-" + String.format("%02d", i) + randomChar() + "-" + String.format("%04d", 1000 + i))
                    .model("Demo Model " + (i % 5))
                    .manufacturer("Demo Manufacturer")
                    .capacity(getCapacityForType(vehicleType))
                    .status(vehicleStatus.name())
                    .isDemoData(true)
                    .build();
            // IMPORTANT: Set isDemoData FIRST, then createdAt to prevent override
            vehicle.setIsDemoData(true);
            vehicle.setCreatedAt(vehicleCreatedAt);
            vehicles.add(vehicleEntityService.save(vehicle));
        }

        log.info("Created {} demo vehicles distributed across months", vehicles.size());
        return vehicles;
    }

    /**
     * Generate monthly data with seasonal distribution
     */
    private Map<String, Integer> generateMonthlyData(
            GenerateDemoDataRequest request,
            List<CustomerEntity> customers,
            List<DriverEntity> drivers,
            List<UserEntity> staffUsers,
            List<VehicleEntity> vehicles) {

        Map<String, Integer> counts = new HashMap<>();
        counts.put("orders", 0);
        counts.put("orderDetails", 0);
        counts.put("assignments", 0);
        counts.put("contracts", 0);
        counts.put("transactions", 0);
        counts.put("refunds", 0);
        counts.put("issues", 0);
        counts.put("maintenances", 0);
        counts.put("penalties", 0);
        counts.put("fuelConsumptions", 0);

        int year = request.getYear();

        for (int month = 1; month <= 12; month++) {
            double seasonalMultiplier = SEASONAL_MULTIPLIERS.get(month);
            int baseCount = randomInt(request.getMinPerMonth(), request.getMaxPerMonth());
            int adjustedCount = (int) Math.ceil(baseCount * seasonalMultiplier);

            log.info("📅 Generating data for month {}/{} - Count: {} (base: {}, multiplier: {})",
                    month, year, adjustedCount, baseCount, seasonalMultiplier);

            Map<String, Integer> monthCounts = generateDataForMonth(
                    year, month, adjustedCount, request, customers, drivers, staffUsers, vehicles);

            monthCounts.forEach((key, value) -> counts.merge(key, value, Integer::sum));
        }

        return counts;
    }

    private Map<String, Integer> generateDataForMonth(
            int year, int month, int count,
            GenerateDemoDataRequest request,
            List<CustomerEntity> customers,
            List<DriverEntity> drivers,
            List<UserEntity> staffUsers,
            List<VehicleEntity> vehicles) {

        Map<String, Integer> counts = new HashMap<>();
        int ordersCreated = 0, orderDetailsCreated = 0, assignmentsCreated = 0;
        int contractsCreated = 0, transactionsCreated = 0, refundsCreated = 0;
        int issuesCreated = 0, maintenancesCreated = 0, penaltiesCreated = 0, fuelConsumptionsCreated = 0;

        // Track which issue categories we've created this month
        Set<IssueCategoryEnum> createdIssueCategories = new HashSet<>();

        for (int i = 0; i < count; i++) {
            try {
                // Create Order - distribute customers evenly for top customer stats
                CustomerEntity customer;
                if (request.getTargetCustomerId() != null) {
                    // 60% chance to use target customer, 40% random for diversity
                    if (randomPercent(60)) {
                        customer = customers.stream()
                                .filter(c -> c.getId().equals(request.getTargetCustomerId()))
                                .findFirst()
                                .orElse(randomElement(customers));
                    } else {
                        customer = randomElement(customers);
                    }
                } else {
                    // Distribute evenly across all customers for better top customer statistics
                    customer = customers.get(i % customers.size());
                }
                LocalDateTime orderDate = randomDateTimeInMonth(year, month);

                OrderEntity order = createDemoOrder(customer, orderDate);
                ordersCreated++;

                // Create 1-5 OrderDetails per order
                int detailCount = randomInt(1, 5);
                List<OrderDetailEntity> orderDetails = new ArrayList<>();

                for (int j = 0; j < detailCount; j++) {
                    OrderDetailEntity detail = createDemoOrderDetail(order, j);
                    orderDetails.add(detail);
                    orderDetailsCreated++;

                    // Create VehicleAssignment - distribute drivers evenly for top driver stats
                    DriverEntity driver;
                    if (request.getTargetDriverId() != null) {
                        // 60% chance to use target driver, 40% random for diversity
                        if (randomPercent(60)) {
                            driver = drivers.stream()
                                    .filter(d -> d.getId().equals(request.getTargetDriverId()))
                                    .findFirst()
                                    .orElse(randomElement(drivers));
                        } else {
                            driver = randomElement(drivers);
                        }
                    } else {
                        // Distribute evenly across all drivers using global counter for better top driver statistics
                        driver = drivers.get(globalDriverCounter % drivers.size());
                        globalDriverCounter++;
                    }
                    VehicleEntity vehicle = randomElement(vehicles);
                    VehicleAssignmentEntity assignment = createDemoAssignment(detail, driver, vehicle, orderDate);
                    assignmentsCreated++;

                    // 30% chance of fuel consumption record
                    if (randomPercent(30)) {
                        createDemoFuelConsumption(assignment);
                        fuelConsumptionsCreated++;
                    }

                    // 15% chance of issue (ensure we create all categories across the month)
                    if (randomPercent(15) || createdIssueCategories.size() < IssueCategoryEnum.values().length) {
                        IssueCategoryEnum category = selectIssueCategory(createdIssueCategories);
                        createDemoIssue(assignment, detail, orderDate, category, staffUsers);
                        createdIssueCategories.add(category);
                        issuesCreated++;
                    }

                    // 10% chance of penalty for driver
                    if (randomPercent(10)) {
                        createDemoPenalty(driver, assignment, orderDate);
                        penaltiesCreated++;
                    }
                }

                // Create Contract for order
                ContractEntity contract = createDemoContract(order, orderDate);
                contractsCreated++;

                // Create single transaction for PAID contracts (full payment)
                if ("PAID".equals(contract.getStatus())) {
                    createDemoTransaction(contract, orderDate.plusDays(1));
                    transactionsCreated++;
                }

                // 15% chance of refund only for PAID contracts
                if ("PAID".equals(contract.getStatus()) && randomPercent(15)) {
                    createDemoRefund(contract, orderDate.plusDays(7));
                    refundsCreated++;
                }

            } catch (Exception e) {
                log.error("Error creating demo data bundle: {}", e.getMessage());
            }
        }

        // Create some maintenances for vehicles in this month
        int maintenanceCount = Math.max(1, count / 10);
        for (int i = 0; i < maintenanceCount; i++) {
            try {
                VehicleEntity vehicle = randomElement(vehicles);
                LocalDateTime maintenanceDate = randomDateTimeInMonth(year, month);
                createDemoMaintenance(vehicle, maintenanceDate);
                maintenancesCreated++;
            } catch (Exception e) {
                log.error("Error creating maintenance: {}", e.getMessage());
            }
        }

        counts.put("orders", ordersCreated);
        counts.put("orderDetails", orderDetailsCreated);
        counts.put("assignments", assignmentsCreated);
        counts.put("contracts", contractsCreated);
        counts.put("transactions", transactionsCreated);
        counts.put("refunds", refundsCreated);
        counts.put("issues", issuesCreated);
        counts.put("maintenances", maintenancesCreated);
        counts.put("penalties", penaltiesCreated);
        counts.put("fuelConsumptions", fuelConsumptionsCreated);

        return counts;
    }

    // ===== Entity Creation Methods =====

    private OrderEntity createDemoOrder(CustomerEntity customer, LocalDateTime createdAt) {
        OrderEntity order = OrderEntity.builder()
                .sender(customer)
                .orderCode("DEMO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .receiverName(randomElement(RECEIVER_NAMES))
                .receiverPhone("09" + String.format("%08d", randomInt(40000000, 49999949)))
                .packageDescription("Demo package description")
                .status("PENDING")
                .isDemoData(true)
                .build();
        
        // CRITICAL: Set isDemoData FIRST, then createdAt
        order.setIsDemoData(true);
        order.setCreatedAt(createdAt);
        
        // Use repository.save() for better performance - @CreatedDate is disabled
        return orderEntityService.save(order);
    }

    private OrderDetailEntity createDemoOrderDetail(OrderEntity order, int index) {
        String[] possibleStatuses = {"DELIVERED", "SUCCESSFUL", "PENDING", "ON_DELIVERED", "CANCELLED", "IN_TROUBLES"};
        OrderDetailEntity detail = OrderDetailEntity.builder()
                .orderEntity(order)
                .trackingCode("TRK-DEMO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .status(randomElement(possibleStatuses))
                .weightTons(BigDecimal.valueOf(randomInt(1, 10) / 10.0)) // 0.1 to 1.0 tons
                .description("Demo package " + (index + 1))
                .isDemoData(true)
                .build();
        // IMPORTANT: Set isDemoData FIRST, then createdAt to prevent override
        detail.setIsDemoData(true);
        detail.setCreatedAt(order.getCreatedAt());
        
        // Use repository.save() for better performance - @CreatedDate is disabled
        return orderDetailEntityService.save(detail);
    }

    private VehicleAssignmentEntity createDemoAssignment(
            OrderDetailEntity detail, DriverEntity driver, VehicleEntity vehicle, LocalDateTime baseDate) {
        VehicleAssignmentEntity assignment = VehicleAssignmentEntity.builder()
                .driver1(driver)
                .vehicleEntity(vehicle)
                .trackingCode("ASG-DEMO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .status(randomElement(new String[]{"COMPLETED", "IN_PROGRESS", "PENDING"}))
                .description("Demo assignment")
                .isDemoData(true)
                .build();
        // IMPORTANT: Set isDemoData FIRST, then createdAt to prevent override
        assignment.setIsDemoData(true);
        assignment.setCreatedAt(baseDate);
        
        // Use repository.save() for better performance - @CreatedDate is disabled
        VehicleAssignmentEntity saved = vehicleAssignmentEntityService.save(assignment);

        // Link to order detail
        detail.setVehicleAssignmentEntity(saved);
        orderDetailEntityService.save(detail);

        return saved;
    }

    private ContractEntity createDemoContract(OrderEntity order, LocalDateTime createdAt) {
        BigDecimal totalValue = BigDecimal.valueOf(randomInt(5000000, 50000000));
        
        // Weighted distribution: 70% PAID, 20% CONTRACT_DRAFT, 10% CANCELLED
        String contractStatus;
        int statusRoll = randomInt(1, 100);
        if (statusRoll <= 70) {
            contractStatus = "PAID";
        } else if (statusRoll <= 90) {
            contractStatus = "CONTRACT_DRAFT";
        } else {
            contractStatus = "CANCELLED";
        }
        
        ContractEntity contract = ContractEntity.builder()
                .orderEntity(order)
                .contractName("Demo Contract " + order.getOrderCode())
                .totalValue(totalValue)
                .adjustedValue(totalValue)
                .effectiveDate(createdAt)
                .expirationDate(createdAt.plusMonths(6))
                .status(contractStatus)
                .isDemoData(true)
                .build();
        // IMPORTANT: Set isDemoData FIRST, then createdAt to prevent override
        contract.setIsDemoData(true);
        contract.setCreatedAt(createdAt);
        
        // Use repository.save() for better performance - @CreatedDate is disabled
        return contractEntityService.save(contract);
    }

    private TransactionEntity createDemoTransaction(ContractEntity contract, LocalDateTime paymentDate) {
        // For PAID contracts, transaction amount should cover contract value
        // Use contract totalValue as base to ensure totalPaid >= totalContractValue
        BigDecimal contractValue = contract.getTotalValue() != null ? contract.getTotalValue() : BigDecimal.valueOf(10000000);
        BigDecimal transactionAmount = contractValue.multiply(BigDecimal.valueOf(randomInt(100, 120))).divide(BigDecimal.valueOf(100));
        
        TransactionEntity transaction = TransactionEntity.builder()
                .contractEntity(contract)
                .gatewayOrderCode("TXN-DEMO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .amount(transactionAmount)
                .paymentDate(paymentDate)
                .paymentProvider(randomElement(new String[]{"VNPAY", "MOMO", "ZALOPAY"}))
                .transactionType("FULL_PAYMENT")
                .currencyCode("VND")
                .status("PAID")
                .isDemoData(true)
                .build();
        // IMPORTANT: Set isDemoData FIRST, then createdAt to prevent override
        transaction.setIsDemoData(true);
        transaction.setCreatedAt(paymentDate);
        
        // Use repository.save() for better performance - @CreatedDate is disabled
        return transactionEntityService.save(transaction);
    }

    private RefundEntity createDemoRefund(ContractEntity contract, LocalDateTime refundDate) {
        RefundEntity refund = RefundEntity.builder()
                .transactionCode("RFD-DEMO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .refundAmount(BigDecimal.valueOf(randomInt(500000, 5000000)))
                .refundDate(refundDate)
                .notes("Demo refund reason")
                .bankName("Demo Bank")
                .accountNumber("1234567890")
                .accountHolderName("Demo Account Holder")
                .isDemoData(true)
                .build();
        // IMPORTANT: Set isDemoData FIRST, then createdAt to prevent override
        refund.setIsDemoData(true);
        refund.setCreatedAt(refundDate);
        return refundEntityService.save(refund);
    }

    // Global counters for even distribution to avoid per-order reset
    private static int globalStaffCounter = 0;
    private static int globalDriverCounter = 0;
    
    private IssueEntity createDemoIssue(
            VehicleAssignmentEntity assignment, OrderDetailEntity detail, 
            LocalDateTime reportedAt, IssueCategoryEnum category, List<UserEntity> staffUsers) {
        LocalDateTime issueReportedAt = reportedAt.plusHours(randomInt(1, 48));
        
        // Distribute staff evenly with 20% random override to avoid perfect patterns
        UserEntity assignedStaff = null;
        if (!staffUsers.isEmpty()) {
            if (randomPercent(80)) {
                // Use modulo cycling with global counter for even distribution
                assignedStaff = staffUsers.get(globalStaffCounter % staffUsers.size());
                globalStaffCounter++;
            } else {
                // 20% random override
                assignedStaff = randomElement(staffUsers);
            }
        }
        
        // Fetch appropriate IssueTypeEntity for the category
        IssueTypeEntity issueType = null;
        try {
            List<IssueTypeEntity> issueTypes = issueTypeEntityService.findAll();
            if (!issueTypes.isEmpty()) {
                // Filter issue types by category if possible, otherwise random
                List<IssueTypeEntity> matchingTypes = issueTypes.stream()
                        .filter(type -> type.getIssueCategory() == null || type.getIssueCategory().equals(category.name()))
                        .collect(Collectors.toList());
                
                if (!matchingTypes.isEmpty()) {
                    issueType = randomElement(matchingTypes);
                } else {
                    issueType = randomElement(issueTypes);
                }
            }
        } catch (Exception e) {
            log.warn("Could not fetch issue types: {}", e.getMessage());
        }
        
        IssueEntity.IssueEntityBuilder<?, ?> issueBuilder = IssueEntity.builder()
                .vehicleAssignmentEntity(assignment)
                .description("Demo " + category.name().toLowerCase().replace("_", " ") + " issue - " + category.name())
                .reportedAt(issueReportedAt)
                .status(randomElement(new String[]{"OPEN", "IN_PROGRESS", "RESOLVED"}))
                .isDemoData(true);
        
        // Assign staff for tracking top staff performance
        if (assignedStaff != null) {
            issueBuilder.staff(assignedStaff);
        }
        
        // Assign issue type if available
        if (issueType != null) {
            issueBuilder.issueTypeEntity(issueType);
        }
        
        IssueEntity issue = issueBuilder.build();
        // IMPORTANT: Set isDemoData FIRST, then createdAt to prevent override
        issue.setIsDemoData(true);
        issue.setCreatedAt(issueReportedAt);
        
        // Set resolvedAt for RESOLVED issues (70% of RESOLVED issues should have resolvedAt)
        if ("RESOLVED".equals(issue.getStatus()) && randomPercent(100)) {
            issue.setResolvedAt(issueReportedAt.plusHours(randomInt(1, 72))); // Resolved within 1-72 hours
        }
        
        // Use repository.save() for better performance - @CreatedDate is disabled
        IssueEntity saved = issueEntityService.save(issue);

        // Link to order detail if applicable (not for PENALTY and OFF_ROUTE_RUNAWAY)
        if (category != IssueCategoryEnum.PENALTY && category != IssueCategoryEnum.OFF_ROUTE_RUNAWAY) {
            detail.setIssueEntity(saved);
            orderDetailEntityService.save(detail);
        }

        return saved;
    }

    private VehicleMaintenanceEntity createDemoMaintenance(VehicleEntity vehicle, LocalDateTime maintenanceDate) {
        VehicleMaintenanceEntity maintenance = VehicleMaintenanceEntity.builder()
                .vehicleEntity(vehicle)
                .description("Demo maintenance - " + randomElement(new String[]{"Oil change", "Tire replacement", "Brake check"}))
                .maintenanceDate(maintenanceDate)
                .nextMaintenanceDate(maintenanceDate.plusMonths(randomInt(1, 3))) // Next maintenance in 1-3 months
                .Status("COMPLETED")
                .completedAt(maintenanceDate.plusHours(randomInt(2, 8)))
                .isDemoData(true)
                .build();
        // IMPORTANT: Set isDemoData FIRST, then createdAt to prevent override
        maintenance.setIsDemoData(true);
        maintenance.setCreatedAt(maintenanceDate);
        return vehicleMaintenanceEntityService.save(maintenance);
    }

    private void createDemoPenalty(DriverEntity driver, VehicleAssignmentEntity assignment, LocalDateTime penaltyDate) {
        PenaltyHistoryEntity penalty = PenaltyHistoryEntity.builder()
                .issueBy(driver)
                .vehicleAssignmentEntity(assignment)
                .violationType(randomElement(new String[]{"SPEEDING", "WRONG_PARKING", "OVERLOAD"}))
                .penaltyDate(penaltyDate.toLocalDate())
                .isDemoData(true)
                .build();
        // IMPORTANT: Set isDemoData FIRST, then createdAt to prevent override
        penalty.setIsDemoData(true);
        penalty.setCreatedAt(penaltyDate);
        penaltyHistoryRepository.save(penalty);
    }

    private void createDemoFuelConsumption(VehicleAssignmentEntity assignment) {
        VehicleFuelConsumptionEntity fuel = VehicleFuelConsumptionEntity.builder()
                .vehicleAssignmentEntity(assignment)
                .fuelVolume(BigDecimal.valueOf(randomInt(50, 300)))
                .odometerReadingAtStart(BigDecimal.valueOf(randomInt(10000, 50000)))
                .odometerReadingAtEnd(BigDecimal.valueOf(randomInt(50001, 100000)))
                .distanceTraveled(BigDecimal.valueOf(randomInt(100, 500)))
                .dateRecorded(assignment.getCreatedAt())
                .notes("Demo fuel consumption record")
                .isDemoData(true)
                .build();
        // IMPORTANT: Set isDemoData FIRST, then createdAt to prevent override
        fuel.setIsDemoData(true);
        fuel.setCreatedAt(assignment.getCreatedAt());
        vehicleFuelConsumptionEntityService.save(fuel);
    }

    // ===== DELETE METHODS =====
    
    private int deleteDemoFuelConsumptions() {
        List<VehicleFuelConsumptionEntity> demo = vehicleFuelConsumptionRepository.findAll()
                .stream().filter(f -> Boolean.TRUE.equals(f.getIsDemoData()))
                .collect(Collectors.toList());
        vehicleFuelConsumptionRepository.deleteAll(demo);
        log.info("Deleted {} demo fuel consumptions", demo.size());
        return demo.size();
    }
    
    private int deleteDemoPenalties() {
        List<PenaltyHistoryEntity> demo = penaltyHistoryRepository.findAll()
                .stream().filter(p -> Boolean.TRUE.equals(p.getIsDemoData()))
                .collect(Collectors.toList());
        penaltyHistoryRepository.deleteAll(demo);
        log.info("Deleted {} demo penalties", demo.size());
        return demo.size();
    }
    
    private int deleteDemoMaintenances() {
        List<VehicleMaintenanceEntity> demo = vehicleMaintenanceRepository.findAll()
                .stream().filter(m -> Boolean.TRUE.equals(m.getIsDemoData()))
                .collect(Collectors.toList());
        vehicleMaintenanceRepository.deleteAll(demo);
        log.info("Deleted {} demo maintenances", demo.size());
        return demo.size();
    }
    
    private int deleteDemoRefunds() {
        List<RefundEntity> demo = refundRepository.findAll()
                .stream().filter(r -> Boolean.TRUE.equals(r.getIsDemoData()))
                .collect(Collectors.toList());
        refundRepository.deleteAll(demo);
        log.info("Deleted {} demo refunds", demo.size());
        return demo.size();
    }
    
    private int deleteDemoTransactions() {
        List<TransactionEntity> demo = transactionRepository.findAll()
                .stream().filter(t -> Boolean.TRUE.equals(t.getIsDemoData()))
                .collect(Collectors.toList());
        transactionRepository.deleteAll(demo);
        log.info("Deleted {} demo transactions", demo.size());
        return demo.size();
    }
    
    private int deleteDemoIssues() {
        List<IssueEntity> demo = issueRepository.findAll()
                .stream().filter(i -> Boolean.TRUE.equals(i.getIsDemoData()))
                .collect(Collectors.toList());
        issueRepository.deleteAll(demo);
        log.info("Deleted {} demo issues", demo.size());
        return demo.size();
    }
    
    private int deleteDemoVehicleAssignments() {
        List<VehicleAssignmentEntity> demo = vehicleAssignmentRepository.findAll()
                .stream().filter(a -> Boolean.TRUE.equals(a.getIsDemoData()))
                .collect(Collectors.toList());
        vehicleAssignmentRepository.deleteAll(demo);
        log.info("Deleted {} demo vehicle assignments", demo.size());
        return demo.size();
    }
    
    private int deleteDemoOrderDetails() {
        List<OrderDetailEntity> demo = orderDetailRepository.findAll()
                .stream().filter(od -> Boolean.TRUE.equals(od.getIsDemoData()))
                .collect(Collectors.toList());
        orderDetailRepository.deleteAll(demo);
        log.info("Deleted {} demo order details", demo.size());
        return demo.size();
    }
    
    private int deleteDemoContracts() {
        List<ContractEntity> demo = contractRepository.findAll()
                .stream().filter(c -> Boolean.TRUE.equals(c.getIsDemoData()))
                .collect(Collectors.toList());
        contractRepository.deleteAll(demo);
        log.info("Deleted {} demo contracts", demo.size());
        return demo.size();
    }
    
    private int deleteDemoOrders() {
        List<OrderEntity> demo = orderRepository.findAll()
                .stream().filter(o -> Boolean.TRUE.equals(o.getIsDemoData()))
                .collect(Collectors.toList());
        orderRepository.deleteAll(demo);
        log.info("Deleted {} demo orders", demo.size());
        return demo.size();
    }
    
    private int deleteDemoVehicles() {
        List<VehicleEntity> demo = vehicleRepository.findAll()
                .stream().filter(v -> Boolean.TRUE.equals(v.getIsDemoData()))
                .collect(Collectors.toList());
        vehicleRepository.deleteAll(demo);
        log.info("Deleted {} demo vehicles", demo.size());
        return demo.size();
    }
    
    private int deleteDemoCustomers() {
        List<CustomerEntity> demo = customerRepository.findAll()
                .stream().filter(c -> Boolean.TRUE.equals(c.getIsDemoData()))
                .collect(Collectors.toList());
        customerRepository.deleteAll(demo);
        log.info("Deleted {} demo customers", demo.size());
        return demo.size();
    }
    
    private int deleteDemoDrivers() {
        List<DriverEntity> demo = driverRepository.findAll()
                .stream().filter(d -> Boolean.TRUE.equals(d.getIsDemoData()))
                .collect(Collectors.toList());
        driverRepository.deleteAll(demo);
        log.info("Deleted {} demo drivers", demo.size());
        return demo.size();
    }
    
    private int deleteDemoUsers() {
        List<UserEntity> demo = userRepository.findAll()
                .stream().filter(u -> Boolean.TRUE.equals(u.getIsDemoData()))
                .collect(Collectors.toList());
        userRepository.deleteAll(demo);
        log.info("Deleted {} demo users", demo.size());
        return demo.size();
    }
    
    // Additional delete methods for other entities (simplified)
    private int deleteDemoIssueImages() { return 0; }
    private int deleteDemoPhotoCompletion() { return 0; }
    private int deleteDemoPackingProofImages() { return 0; }
    private int deleteDemoOffRouteEvents() { return 0; }
    private int deleteDemoNotifications() { return 0; }
    private int deleteDemoFcmTokens() { return 0; }
    private int deleteDemoDevices() { return 0; }
    private int deleteDemoChatReadStatuses() { return 0; }
    private int deleteDemoChatMessages() { return 0; }
    private int deleteDemoChatConversations() { return 0; }
    private int deleteDemoRefreshTokens() { return 0; }
    private int deleteDemoAddresses() { return 0; }
    private int deleteDemoJourneySegments() { return 0; }
    private int deleteDemoJourneyHistory() { return 0; }
    private int deleteDemoSeals() { return 0; }
    private int deleteDemoVehicleReservations() { return 0; }
    private int deleteDemoContractRules() { return 0; }
    
    // ===== Helper Methods =====

    private IssueCategoryEnum selectIssueCategory(Set<IssueCategoryEnum> created) {
        // First, try to create categories we haven't created yet
        List<IssueCategoryEnum> remaining = Arrays.stream(IssueCategoryEnum.values())
                .filter(cat -> !created.contains(cat))
                .collect(Collectors.toList());

        if (!remaining.isEmpty()) {
            return randomElement(remaining);
        }

        // If all created, pick random
        return randomElement(Arrays.asList(IssueCategoryEnum.values()));
    }

    private LocalDateTime randomDateTimeInMonth(int year, int month) {
        LocalDate firstDay = LocalDate.of(year, month, 1);
        LocalDate lastDay = firstDay.withDayOfMonth(firstDay.lengthOfMonth());
        int randomDay = randomInt(1, lastDay.getDayOfMonth());
        int randomHour = randomInt(0, 23);
        int randomMinute = randomInt(0, 59);
        return LocalDateTime.of(year, month, randomDay, randomHour, randomMinute);
    }

    private LocalDateTime randomDateTimeInYear(int year) {
        int randomMonth = randomInt(1, 12);
        return randomDateTimeInMonth(year, randomMonth);
    }

    private String randomAddress() {
        String city = randomElement(CITIES);
        int streetNum = randomInt(1, 999);
        return streetNum + " Đường Demo, " + city;
    }

    private int randomInt(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    private boolean randomPercent(int percent) {
        return randomInt(1, 100) <= percent;
    }

    private <T> T randomElement(T[] array) {
        return array[randomInt(0, array.length - 1)];
    }

    private <T> T randomElement(List<T> list) {
        return list.get(randomInt(0, list.size() - 1));
    }

    private char randomChar() {
        return (char) ('A' + randomInt(0, 25));
    }

    private BigDecimal getCapacityForType(String vehicleType) {
        return switch (vehicleType) {
            case "TRUCK_1_TON" -> BigDecimal.valueOf(1000);
            case "TRUCK_2_TON" -> BigDecimal.valueOf(2000);
            case "TRUCK_3_5_TON" -> BigDecimal.valueOf(3500);
            case "TRUCK_5_TON" -> BigDecimal.valueOf(5000);
            case "TRUCK_8_TON" -> BigDecimal.valueOf(8000);
            case "TRUCK_10_TON" -> BigDecimal.valueOf(10000);
            case "TRUCK_15_TON" -> BigDecimal.valueOf(15000);
            case "TRUCK_20_TON" -> BigDecimal.valueOf(20000);
            default -> BigDecimal.valueOf(5000);
        };
    }
}
