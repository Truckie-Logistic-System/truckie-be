package capstone_project.service.services.vehicle.impl;

import capstone_project.common.enums.*;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.request.vehicle.*;
import capstone_project.dtos.response.order.ListContractRuleAssignResult;
import capstone_project.dtos.response.order.contract.ContractRuleAssignResponse;
import capstone_project.dtos.response.user.DriverResponse;
import capstone_project.dtos.response.vehicle.*;
import capstone_project.entity.order.contract.ContractEntity;
import capstone_project.entity.order.order.*;
import capstone_project.entity.pricing.VehicleTypeRuleEntity;
import capstone_project.entity.user.address.AddressEntity;
import capstone_project.entity.user.driver.DriverEntity;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.entity.vehicle.VehicleEntity;
import capstone_project.repository.entityServices.order.contract.ContractEntityService;
import capstone_project.repository.entityServices.order.order.JourneyHistoryEntityService;
import capstone_project.repository.entityServices.order.order.OrderDetailEntityService;
import capstone_project.repository.entityServices.order.order.OrderEntityService;
import capstone_project.repository.entityServices.order.order.SealEntityService;
import capstone_project.repository.entityServices.pricing.VehicleTypeRuleEntityService;
import capstone_project.repository.entityServices.user.DriverEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleAssignmentEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleTypeEntityService;
import capstone_project.repository.repositories.user.PenaltyHistoryRepository;
import capstone_project.service.mapper.user.DriverMapper;
import capstone_project.service.mapper.vehicle.VehicleAssignmentMapper;
import capstone_project.service.mapper.vehicle.VehicleMapper;
import capstone_project.service.services.order.order.*;
import capstone_project.service.services.thirdPartyServices.Vietmap.VietmapService;
import capstone_project.service.services.user.DriverService;
import capstone_project.service.services.vehicle.VehicleAssignmentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleAssignmentServiceImpl implements VehicleAssignmentService {

    private final VehicleAssignmentEntityService entityService;
    private final VehicleTypeEntityService vehicleTypeEntityService;
    private final DriverEntityService driverEntityService;
    private final VehicleEntityService vehicleEntityService;
    private final OrderEntityService orderEntityService;
    private final OrderDetailEntityService orderDetailEntityService;
    private final ContractEntityService contractEntityService;
    private final ContractRuleService contractRuleService;
    private final VehicleTypeRuleEntityService vehicleTypeRuleEntityService;
    private final DriverService driverService;
    private final VehicleAssignmentMapper mapper;
    private final VehicleMapper vehicleMapper;
    private final DriverMapper driverMapper;
    private final PenaltyHistoryRepository penaltyHistoryRepository;
    private final ContractService contractService;
    private final OrderService orderService;
    private final OrderDetailService orderDetailService;
    private final JourneyHistoryEntityService journeyHistoryEntityService;
    private final VietmapService vietmapService;
    private final SealEntityService sealEntityService;

    private final ObjectMapper objectMapper;

    @Value("${prefix.vehicle.assignment.code}")
    private String prefixVehicleAssignmentCode;

    /**
     * Define custom error codes for vehicle and driver availability
     * These constants are missing from ErrorEnum
     */
    private static final long VEHICLE_NOT_AVAILABLE = 30;
    private static final long DRIVER_NOT_AVAILABLE = 31;

    @Override
    public List<VehicleAssignmentResponse> getAllAssignments() {
        log.info("Fetching all vehicles");
        return Optional.of(entityService.findAll())
                .filter(list -> !list.isEmpty())
                .orElseThrow(() -> new NotFoundException(
                        "There are no vehicle assignments available.",
                        ErrorEnum.NOT_FOUND.getErrorCode()))
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    public VehicleAssignmentResponse getAssignmentById(UUID id) {
        log.info("Fetching vehicle assignment by ID: {}", id);
        VehicleAssignmentEntity entity = entityService.findEntityById(id)
                .orElseThrow(() -> new NotFoundException(
                        "Assignment is not found with ASSIGNMENT ID: " + id,
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));
        return mapper.toResponse(entity);
    }

    @Override
    public VehicleAssignmentResponse createAssignment(VehicleAssignmentRequest req) {
        log.info("Creating new vehicle assignment");

        vehicleEntityService.findByVehicleId(UUID.fromString(req.vehicleId())).orElseThrow(() -> new NotFoundException(
                ErrorEnum.VEHICLE_NOT_FOUND.getMessage(),
                ErrorEnum.VEHICLE_NOT_FOUND.getErrorCode()
        ));

        driverEntityService.findEntityById(UUID.fromString(req.driverId_1())).orElseThrow(() -> new NotFoundException(
                "Driver 1 not found with DRIVER ID: " + req.driverId_1(),
                ErrorEnum.VEHICLE_NOT_FOUND.getErrorCode()
        ));

        driverEntityService.findEntityById(UUID.fromString(req.driverId_2())).orElseThrow(() -> new NotFoundException(
                "Driver 2 not found with DRIVER ID: " + req.driverId_2(),
                ErrorEnum.VEHICLE_NOT_FOUND.getErrorCode()
        ));

        var saved = entityService.save(mapper.toEntity(req));


        return mapper.toResponse(saved);
    }

    @Override
    public VehicleAssignmentResponse updateAssignment(UUID id, UpdateVehicleAssignmentRequest req) {
        log.info("Updating vehicle assignment with ID: {}", id);
        var existing = entityService.findEntityById(id)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Assignment is not found with ASSIGNMENT ID: " + id,
                                ErrorEnum.NOT_FOUND.getErrorCode()));

        mapper.toEntity(req, existing);
        var updated = entityService.save(existing);
        return mapper.toResponse(updated);
    }

    @Override
    public List<VehicleAssignmentResponse> getAllAssignmentsWithOrder(UUID vehicleType) {
        log.info("Fetching vehicle assignment by vehicle type ID: {}", vehicleType);

        vehicleTypeEntityService.findEntityById(vehicleType)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.VEHICLE_TYPE_NOT_FOUND.getMessage(),
                        ErrorEnum.VEHICLE_TYPE_NOT_FOUND.getErrorCode()
                ));


        List<VehicleAssignmentEntity> entity = entityService.findVehicleWithOrder(vehicleType);
        if (entity.isEmpty()) {
            throw new NotFoundException(ErrorEnum.NO_VEHICLE_AVAILABLE.getMessage(),
                    ErrorEnum.NO_VEHICLE_AVAILABLE.getErrorCode());
        }
        return entity.stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    public List<VehicleAssignmentResponse> getListVehicleAssignmentByOrderID(UUID orderID) {
        log.info("Fetching vehicle assignment by vehicle type ID: {}", orderID);

        orderEntityService.findEntityById(orderID)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));


        List<VehicleAssignmentEntity> entity = entityService.findVehicleAssignmentsWithOrderID(orderID);
        if (entity.isEmpty()) {
            throw new NotFoundException(ErrorEnum.NO_VEHICLE_AVAILABLE.getMessage(),
                    ErrorEnum.NO_VEHICLE_AVAILABLE.getErrorCode());
        }
        return entity.stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    public List<SampleVehicleAssignmentResponse> getVehicleAndDriversForDetails(UUID orderID) {
        List<SampleVehicleAssignmentResponse> sampleVehicleAssignmentResponses = new ArrayList<>();

        OrderEntity order = orderEntityService.findEntityById(orderID)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage() + " Order",
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        ContractEntity contractEntity = contractEntityService.getContractByOrderId(orderID)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage() + "Contract",
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        // lay goi y assign Vehicle type tu contract rules
        ListContractRuleAssignResult assignResult =
                contractRuleService.getListAssignOrUnAssignContractRule(contractEntity.getId());

        if (!order.getStatus().equals(OrderStatusEnum.ON_PLANNING.name())) {
            throw new NotFoundException("Đơn hàng chưa phải là ON_PLANNING",
                    ErrorEnum.NO_VEHICLE_AVAILABLE.getErrorCode());
        }

        for (ContractRuleAssignResponse response : assignResult.vehicleAssignments()) {
            UUID vehicleRuleId = response.getVehicleTypeRuleId();
            VehicleTypeRuleEntity vehicleRule = vehicleTypeRuleEntityService.findEntityById(vehicleRuleId)
                    .orElseThrow(() -> new NotFoundException(
                            "Vehicle rule not found: " + vehicleRuleId,
                            ErrorEnum.NOT_FOUND.getErrorCode()
                    ));
            VehicleTypeEnum vehicleTypeEnum = VehicleTypeEnum.valueOf(vehicleRule.getVehicleTypeEntity().getVehicleTypeName());
            List<VehicleEntity> getVehiclesByVehicleType = vehicleEntityService.getVehicleEntitiesByVehicleTypeEntityAndStatus(vehicleRule.getVehicleTypeEntity(), CommonStatusEnum.ACTIVE.name());
            log.info("Tìm thấy {} xe ACTIVE cho loại {}", getVehiclesByVehicleType.size(), vehicleTypeEnum);

            // Lấy tất cả các tài xế hợp lệ cho loại xe này
            List<DriverEntity> allEligibleDrivers = driverEntityService.findByStatus(CommonStatusEnum.ACTIVE.name())
                    .stream()
                    .filter(d -> driverService.isCheckClassDriverLicenseForVehicleType(d, vehicleTypeEnum))
                    .filter(d -> !entityService.existsActiveAssignmentForDriver(d.getId()))
                    .toList();
            log.info("Tìm thấy {} tài xế ACTIVE hợp lệ cho loại xe {}", allEligibleDrivers.size(), vehicleTypeEnum);

            // Giới hạn số lượng xe để tránh quá nhiều gợi ý
            final int MAX_VEHICLES_PER_DETAIL = 5;

            // Sắp xếp xe theo mức độ sử dụng (ít dùng nhất lên đầu)
            List<UUID> vehicleIds = getVehiclesByVehicleType.stream().map(VehicleEntity::getId).toList();
            List<UUID> sortedVehicleIds = sortVehiclesByUsageThisMonth(vehicleIds);

            // Tạo một map để giữ các vehicle assignment cho order detail này
            Map<VehicleResponse, List<DriverResponse>> detailVehicleAssignments = new HashMap<>();

            int vehicleCount = 0;
            for (UUID vehicleId : sortedVehicleIds) {
                // Giới hạn số lượng xe
                if (vehicleCount >= MAX_VEHICLES_PER_DETAIL) {
                    break;
                }

                VehicleEntity vehicle = vehicleEntityService.findEntityById(vehicleId).get();
                Optional<VehicleAssignmentEntity> activeAssignment = entityService.findVehicleAssignmentByVehicleEntityAndStatus(vehicle, CommonStatusEnum.ACTIVE.name());

                if (activeAssignment.isPresent()) {
                    continue;  // Bỏ qua xe đang có assignment
                }

                // Ưu tiên tài xế từ assignment gần đây nhất nếu có
                List<DriverEntity> preferredDrivers = new ArrayList<>();
                List<VehicleAssignmentEntity> pastAssignments = entityService.findAssignmentsByVehicleOrderByCreatedAtDesc(vehicle);

                if (!pastAssignments.isEmpty()) {
                    VehicleAssignmentEntity lastAssignment = pastAssignments.get(0);
                    DriverEntity driver1 = lastAssignment.getDriver1();
                    DriverEntity driver2 = lastAssignment.getDriver2();

                    if (driver1 != null && CommonStatusEnum.ACTIVE.name().equals(driver1.getStatus()) &&
                            driverService.isCheckClassDriverLicenseForVehicleType(driver1, vehicleTypeEnum) &&
                            !entityService.existsActiveAssignmentForDriver(driver1.getId())) {
                        preferredDrivers.add(driver1);
                    }

                    if (driver2 != null && CommonStatusEnum.ACTIVE.name().equals(driver2.getStatus()) &&
                            driverService.isCheckClassDriverLicenseForVehicleType(driver2, vehicleTypeEnum) &&
                            !entityService.existsActiveAssignmentForDriver(driver2.getId()) &&
                            !preferredDrivers.contains(driver2)) {
                        preferredDrivers.add(driver2);
                    }
                }

                // Danh sách tài xế đề xuất cho xe này (ưu tiên + thêm tài xế khác)
                List<DriverEntity> selectedDrivers = new ArrayList<>(preferredDrivers);

                // Thêm tài xế khác từ danh sách tài xế hợp lệ
                // Lấy tối đa 5 tài xế cho mỗi xe
                final int MAX_DRIVERS_PER_VEHICLE = 5;

                for (DriverEntity driver : allEligibleDrivers) {
                    if (selectedDrivers.size() >= MAX_DRIVERS_PER_VEHICLE) break;
                    if (!selectedDrivers.contains(driver)) {
                        selectedDrivers.add(driver);
                    }
                }

                if (!selectedDrivers.isEmpty()) {
                    List<DriverResponse> driverResponses = selectedDrivers.stream()
                            .map(driverMapper::mapDriverResponse)
                            .toList();

                    detailVehicleAssignments.put(vehicleMapper.toResponse(vehicle), driverResponses);
                    vehicleCount++;
                }
            }

            if (!detailVehicleAssignments.isEmpty()) {
                sampleVehicleAssignmentResponses.add(
                        new SampleVehicleAssignmentResponse(
                                response.getAssignedDetails()
                                        .stream()
                                        .map(d -> UUID.fromString(d.id()))
                                        .toList(),
                                detailVehicleAssignments
                        )
                );

            }
        }

        if (sampleVehicleAssignmentResponses.isEmpty()) {
            log.warn("Không tìm được cặp xe và tài xế phù hợp cho order {}", orderID);
        }
        return sampleVehicleAssignmentResponses;
    }

    /**
     * Chuyển đổi response phức tạp sang response đơn giản hóa, nhóm theo order detail ID
     * Thêm đánh dấu cho xe và tài xế phù hợp nhất (isRecommended=true)
     * Bổ sung thông tin từ dữ liệu thực tế: vi phạm, số chuyến, kinh nghiệm, thời gian hoạt động gần nhất
     */
    public SimplifiedVehicleAssignmentResponse convertToSimplifiedResponse(List<SampleVehicleAssignmentResponse> responses) {
        Map<String, List<SimplifiedVehicleAssignmentResponse.VehicleSuggestionDTO>> suggestionsByTrackingCode = new HashMap<>();

        // Thu thập tất cả driver ID để tính số chuyến đã hoàn thành
        Set<UUID> allDriverIds = new HashSet<>();
        for (SampleVehicleAssignmentResponse response : responses) {
            for (List<DriverResponse> driverList : response.sampleVehicleAssignment().values()) {
                for (DriverResponse driver : driverList) {
                    allDriverIds.add(UUID.fromString(driver.getId()));
                }
            }
        }

        // Tính số chuyến đã hoàn thành cho mỗi tài xế
        Map<UUID, Integer> driversCompletedTripsMap = countCompletedTripsByDrivers(allDriverIds);

        // Tìm thời gian hoạt động gần nhất của mỗi tài xế
        Map<UUID, String> driverLastActiveTimeMap = findLastActiveTimeForDrivers(allDriverIds);

        for (SampleVehicleAssignmentResponse response : responses) {
            // Với mỗi order detail ID trong assignedDetails
            for (UUID detailId : response.assignedDetails()) {
                // Lấy tracking code từ order detail ID
                String trackingCode = getTrackingCodeFromDetailId(detailId);

                // Danh sách xe gợi ý cho order detail này
                List<SimplifiedVehicleAssignmentResponse.VehicleSuggestionDTO> detailSuggestions = new ArrayList<>();

                // Lấy danh sách xe từ response
                List<VehicleResponse> vehicles = new ArrayList<>(response.sampleVehicleAssignment().keySet());

                // Xác định xe phù hợp nhất (xe ít được sử dụng nhất - đầu tiên trong danh sách đã sắp xếp)
                VehicleResponse mostSuitableVehicle = vehicles.isEmpty() ? null : vehicles.get(0);

                // Xử lý các vehicle và driver từ response
                for (Map.Entry<VehicleResponse, List<DriverResponse>> entry : response.sampleVehicleAssignment().entrySet()) {
                    VehicleResponse vehicleResponse = entry.getKey();
                    List<DriverResponse> driverResponses = entry.getValue();

                    // Kiểm tra nếu là xe phù hợp nhất
                    boolean isRecommendedVehicle = vehicleResponse.equals(mostSuitableVehicle);

                    // Xác định tài xế phù hợp nhất (2 tài xế đầu tiên trong danh sách)
                    // Tài xế đầu danh sách thường là các tài xế đã từng lái xe này
                    List<String> recommendedDriverIds = driverResponses.stream()
                            .limit(2)
                            .map(DriverResponse::getId)
                            .toList();

                    // Chuyển đổi driver response sang DriverSuggestionDTO với thông tin bổ sung
                    List<SimplifiedVehicleAssignmentResponse.DriverSuggestionDTO> driverSuggestionDTOs = driverResponses.stream()
                            .map(driver -> {
                                // Tính toán số lượng vi phạm
                                int violationCount = driver.getPenaltyHistories() != null ? driver.getPenaltyHistories().size() : 0;

                                // Lấy số chuyến đã hoàn thành từ map
                                UUID driverId = UUID.fromString(driver.getId());
                                int completedTrips = driversCompletedTripsMap.getOrDefault(driverId, 0);

                                // Lấy thời gian hoạt động gần nhất từ map
                                String lastActiveTime = driverLastActiveTimeMap.getOrDefault(driverId, "Chưa có hoạt động");

                                // Tính thời gian làm việc dựa trên ngày tạo hồ sơ của tài xế (createdAt)
                                String workExperience = "Chưa có dữ liệu";

                                try {
                                    // Lấy thông tin createdAt của tài xế từ DriverEntity
                                    DriverEntity driverEntity = driverEntityService.findEntityById(driverId)
                                            .orElse(null);

                                    if (driverEntity != null && driverEntity.getCreatedAt() != null) {
                                        LocalDate joinDate = driverEntity.getCreatedAt().toLocalDate();
                                        LocalDate today = LocalDate.now();

                                        // Tính số năm làm việc
                                        int yearsOfWork = today.getYear() - joinDate.getYear();

                                        // Điều chỉnh nếu chưa đến ngày kỷ niệm hàng năm
                                        if (today.getMonthValue() < joinDate.getMonthValue() ||
                                                (today.getMonthValue() == joinDate.getMonthValue() &&
                                                        today.getDayOfMonth() < joinDate.getDayOfMonth())) {
                                            yearsOfWork--;
                                        }

                                        // Tính số tháng làm việc
                                        int monthsOfWork = today.getMonthValue() - joinDate.getMonthValue();
                                        if (monthsOfWork < 0) monthsOfWork += 12;
                                        if (today.getDayOfMonth() < joinDate.getDayOfMonth()) {
                                            monthsOfWork = (monthsOfWork + 11) % 12; // Điều chỉnh nếu chưa đến ngày kỷ niệm hàng tháng
                                        }

                                        if (yearsOfWork == 0) {
                                            workExperience = monthsOfWork + " tháng";
                                        } else {
                                            workExperience = yearsOfWork + " năm";
                                        }
                                    }
                                } catch (Exception e) {
                                    log.error("Lỗi khi tính thời gian làm việc cho tài xế {}: {}", driverId, e.getMessage());
                                    workExperience = "Chưa có dữ liệu";
                                }

                                return new SimplifiedVehicleAssignmentResponse.DriverSuggestionDTO(
                                        UUID.fromString(driver.getId()),
                                        driver.getUserResponse().getFullName(),
                                        driver.getDriverLicenseNumber(),
                                        driver.getLicenseClass(),
                                        recommendedDriverIds.contains(driver.getId()), // đánh dấu tài xế phù hợp nhất
                                        violationCount,         // Số lần vi phạm
                                        completedTrips,         // Số chuyến đã hoàn thành (từ dữ liệu thực)
                                        workExperience,        // Kinh nghiệm (tính từ dateOfPassing)
                                        lastActiveTime          // Thời gian hoạt động gần nh��t
                                );
                            })
                            .toList();

                    // Tạo VehicleSuggestionDTO với thông tin isRecommended
                    SimplifiedVehicleAssignmentResponse.VehicleSuggestionDTO vehicleSuggestionDTO =
                            new SimplifiedVehicleAssignmentResponse.VehicleSuggestionDTO(
                                    UUID.fromString(vehicleResponse.id()),
                                    vehicleResponse.licensePlateNumber(),
                                    vehicleResponse.model(),
                                    vehicleResponse.manufacturer(),
                                    driverSuggestionDTOs,
                                    isRecommendedVehicle // đánh dấu xe phù hợp nhất
                            );

                    // Thêm vào danh sách gợi ý cho order detail này
                    detailSuggestions.add(vehicleSuggestionDTO);
                }

                // Thêm hoặc cập nhật danh sách gợi ý cho order detail này (sử dụng tracking code)
                if (!detailSuggestions.isEmpty()) {
                    suggestionsByTrackingCode.put(trackingCode, detailSuggestions);
                }
            }
        }

        return new SimplifiedVehicleAssignmentResponse(suggestionsByTrackingCode);
    }

    /**
     * Tìm thời gian hoạt động gần nhất của mỗi tài xế
     *
     * @param driverIds Danh sách ID của tài xế
     * @return Map chứa thông tin thời gian hoạt động gần nhất ở dạng String
     */
    private Map<UUID, String> findLastActiveTimeForDrivers(Set<UUID> driverIds) {
        Map<UUID, String> result = new HashMap<>();

        if (driverIds.isEmpty()) {
            return result;
        }

        for (UUID driverId : driverIds) {
            Optional<VehicleAssignmentEntity> latestAssignment = entityService.findLatestAssignmentByDriverId(driverId);

            if (latestAssignment.isPresent()) {
                VehicleAssignmentEntity assignment = latestAssignment.get();
                LocalDateTime createdAt = assignment.getCreatedAt();

                // Định dạng thời gian hoạt động gần nhất
                LocalDateTime now = LocalDateTime.now();
                LocalDate today = LocalDate.now();
                LocalDate assignmentDate = createdAt.toLocalDate();

                String formattedTime;
                if (assignmentDate.isEqual(today)) {
                    formattedTime = "Hôm nay, lúc " + createdAt.getHour() + ":" + String.format("%02d", createdAt.getMinute());
                } else if (assignmentDate.isEqual(today.minusDays(1))) {
                    formattedTime = "Hôm qua, lúc " + createdAt.getHour() + ":" + String.format("%02d", createdAt.getMinute());
                } else {
                    formattedTime = assignmentDate.getDayOfMonth() + "/" + assignmentDate.getMonthValue() + "/" + assignmentDate.getYear();
                }

                result.put(driverId, formattedTime);
            } else {
                result.put(driverId, "Chưa có hoạt động");
            }
        }

        return result;
    }

    @Override
    public SimplifiedVehicleAssignmentResponse getSimplifiedSuggestionsForOrder(UUID orderID) {
        List<SampleVehicleAssignmentResponse> responses = getVehicleAndDriversForDetails(orderID);
        return convertToSimplifiedResponse(responses);
    }

    /**
     * Tính số chuyến đã hoàn thành của mỗi tài xế
     *
     * @param driverIds Danh sách ID của tài xế
     * @return Map chứa số chuyến đã hoàn thành cho mỗi tài xế
     */
    private Map<UUID, Integer> countCompletedTripsByDrivers(Set<UUID> driverIds) {
        Map<UUID, Integer> result = new HashMap<>();

        if (driverIds.isEmpty()) {
            return result;
        }

        // Với mỗi tài xế, đếm số chuyến đã hoàn thành
        // Tài xế có thể là driver1 hoặc driver2 trong vehicle assignment
        for (UUID driverId : driverIds) {
            // Đếm số chuyến hoàn thành khi là driver1
            int tripsAsDriver1 = entityService.countCompletedTripsAsDriver1(driverId);

            // Đếm số chuyến hoàn thành khi là driver2
            int tripsAsDriver2 = entityService.countCompletedTripsAsDriver2(driverId);

            // Tổng số chuyến hoàn thành
            result.put(driverId, tripsAsDriver1 + tripsAsDriver2);
        }

        return result;
    }

    /**
     * Sắp xếp xe theo số lần sử dụng trong tháng, xe ít sử dụng được ưu tiên
     *
     * @param vehicleIds Danh sách ID xe cần sắp xếp
     * @return Danh sách ID xe đã sắp xếp theo số lần sử dụng (tăng dần)
     */
    private List<UUID> sortVehiclesByUsageThisMonth(List<UUID> vehicleIds) {
        if (vehicleIds == null || vehicleIds.isEmpty()) {
            return List.of();
        }

        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime endOfMonth = startOfMonth.plusMonths(1);

        // Gọi repo 1 lần lấy count cho tất cả xe
        List<Object[]> results = entityService.countAssignmentsThisMonthForVehicles(vehicleIds, startOfMonth, endOfMonth);

        // Chuyển sang map: <vehicleId, count>
        Map<UUID, Long> usageMap = results.stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> (Long) row[1]
                ));

        // Những xe không có trong kết quả query thì count = 0
        for (UUID vehicleId : vehicleIds) {
            usageMap.putIfAbsent(vehicleId, 0L);
        }

        // Sắp xếp tăng dần theo số lần dùng
        return usageMap.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .toList();
    }

    /**
     * Lấy tracking code từ order detail ID
     *
     * @param detailId ID của order detail
     * @return Tracking code tương ứng hoặc null nếu không tìm thấy
     */
    private String getTrackingCodeFromDetailId(UUID detailId) {
        try {
            // Truy vấn OrderDetailEntity từ database để lấy tracking code
            return orderDetailEntityService.findEntityById(detailId)
                    .map(orderDetail -> orderDetail.getTrackingCode())
                    .orElse(detailId.toString()); // Fallback: sử dụng ID làm tracking code nếu không tìm thấy
        } catch (Exception e) {
            log.error("Lỗi khi lấy tracking code cho order detail {}: {}", detailId, e.getMessage());
            return detailId.toString(); // Fallback khi có lỗi
        }
    }

    /**
     * Lấy danh sách gợi ý xe và tài xế cho order với các order detail được nhóm lại
     * Sử dụng cả 2 thuật toán: Optimal (BinPacker) và Realistic (First-Fit + Upgrade)
     * Ưu tiên sử dụng Optimal nếu có, fallback sang Realistic nếu Optimal thất bại
     *
     * @param orderID ID của order
     * @return Danh sách gợi ý với các order detail được nhóm lại thành các chuyến
     */
    @Override
    public GroupedVehicleAssignmentResponse getGroupedSuggestionsForOrder(UUID orderID) {
        log.info("Generating grouped vehicle assignment suggestions for order ID: {}", orderID);
        final long startTime = System.nanoTime();

        OrderEntity order = orderEntityService.findEntityById(orderID)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage() + " Order",
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        if (!order.getStatus().equals(OrderStatusEnum.ON_PLANNING.name())) {
            throw new NotFoundException("Đơn hàng chưa phải là ON_PLANNING",
                    ErrorEnum.NO_VEHICLE_AVAILABLE.getErrorCode());
        }

        // Sử dụng cả 2 thuật toán giống như endpoint /contracts/{orderId}/get-both-optimal-and-realistic-assign-vehicles
        List<ContractRuleAssignResponse> optimalAssignments = null;
        List<ContractRuleAssignResponse> realisticAssignments = null;

        try {
            optimalAssignments = contractService.assignVehiclesOptimal(orderID);
            log.info("[getGroupedSuggestionsForOrder] Optimal assignment succeeded for orderId={}, vehicles used={}",
                    orderID, optimalAssignments.size());
        } catch (Exception e) {
            log.warn("[getGroupedSuggestionsForOrder] Optimal assignment failed for orderId={}, reason={}, fallback to realistic",
                    orderID, e.getMessage());
        }

        try {
            realisticAssignments = contractService.assignVehiclesWithAvailability(orderID);
            log.info("[getGroupedSuggestionsForOrder] Realistic assignment succeeded for orderId={}, vehicles used={}",
                    orderID, realisticAssignments.size());
        } catch (Exception e) {
            log.warn("[getGroupedSuggestionsForOrder] Realistic assignment failed for orderId={}, reason={}",
                    orderID, e.getMessage());
        }

        // Ưu tiên optimal, fallback sang realistic
        List<ContractRuleAssignResponse> vehicleAssignments = optimalAssignments != null ? optimalAssignments : realisticAssignments;

        if (vehicleAssignments == null || vehicleAssignments.isEmpty()) {
            log.error("Không tìm thấy gợi ý phân bổ xe cho đơn hàng ID={}", orderID);
            throw new NotFoundException(
                    "Không tìm thấy gợi ý phân bổ xe cho đơn hàng này",
                    ErrorEnum.NOT_FOUND.getErrorCode()
            );
        }

        // Chuyển đổi kết quả từ assignVehicles thành định dạng OrderDetailGroup
        List<GroupedVehicleAssignmentResponse.OrderDetailGroup> groups =
                convertAssignmentsToGroups(vehicleAssignments);

        long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;
        log.info("Completed generating suggestions for order {} in {} ms using {} algorithm",
                orderID, elapsedMs, optimalAssignments != null ? "OPTIMAL" : "REALISTIC");

        return new GroupedVehicleAssignmentResponse(groups);
    }

    /**
     * Chuyển đổi kết quả từ assignVehicles sang định dạng OrderDetailGroup
     * Giữ nguyên cấu trúc response như trước đây
     */
    private List<GroupedVehicleAssignmentResponse.OrderDetailGroup> convertAssignmentsToGroups(
            List<ContractRuleAssignResponse> assignments) {

        List<GroupedVehicleAssignmentResponse.OrderDetailGroup> groups = new ArrayList<>();

        for (ContractRuleAssignResponse assignment : assignments) {
            // Lấy thông tin về vehicle rule
            UUID vehicleRuleId = assignment.getVehicleTypeRuleId();
            VehicleTypeRuleEntity vehicleRule = vehicleTypeRuleEntityService.findEntityById(vehicleRuleId)
                    .orElseThrow(() -> new NotFoundException(
                            "Vehicle rule not found: " + vehicleRuleId,
                            ErrorEnum.NOT_FOUND.getErrorCode()
                    ));

            List<UUID> detailIds = assignment.getAssignedDetails().stream()
                    .map(detail -> UUID.fromString(detail.id()))
                    .toList();

            if (detailIds.isEmpty()) continue;

            List<GroupedVehicleAssignmentResponse.OrderDetailInfo> detailInfos =
                    getOrderDetailInfos(detailIds);

            List<GroupedVehicleAssignmentResponse.VehicleSuggestionResponse> vehicleSuggestions =
                    findSuitableVehiclesForGroup(detailIds, vehicleRule, vehicleRule.getVehicleTypeEntity() != null
                            ? vehicleRule.getVehicleTypeEntity().getId() : null);

            BigDecimal totalWeight = detailIds.stream()
                    .map(id -> orderDetailEntityService.findEntityById(id).orElse(null))
                    .filter(Objects::nonNull)
                    .map(OrderDetailEntity::getWeight)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);


            // Xác định lý do nhóm
            String groupingReason;
            if (detailIds.size() == 1) {
                groupingReason = "Đơn hàng không thể gộp do kích thước hoặc trọng lượng lớn";
            } else {
                groupingReason = String.format(
                        "Các đơn hàng được gộp tối ưu cho xe %s (%.1f/%.1f kg - %.1f%%)",
                        vehicleRule.getVehicleTypeRuleName(),
                        totalWeight.doubleValue(),
                        vehicleRule.getMaxWeight().doubleValue(),
                        totalWeight.doubleValue() * 100 / vehicleRule.getMaxWeight().doubleValue()
                );
            }

            // Tạo nhóm mới
            GroupedVehicleAssignmentResponse.OrderDetailGroup group =
                    new GroupedVehicleAssignmentResponse.OrderDetailGroup(
                            detailInfos, vehicleSuggestions, groupingReason);

            groups.add(group);
        }

        return groups;
    }

    /**
     * Implementation of createGroupedAssignments from the interface
     * Creates vehicle assignments for groups of order details
     */
    @Override
    public List<VehicleAssignmentResponse> createGroupedAssignments(GroupedAssignmentRequest request) {
        log.info("Creating grouped vehicle assignments for {} groups", request.groupAssignments().size());
        
        // VALIDATION: Check all groups have required data
        List<String> validationErrors = new ArrayList<>();
        
        for (int i = 0; i < request.groupAssignments().size(); i++) {
            OrderDetailGroupAssignment groupAssignment = request.groupAssignments().get(i);
            int groupNumber = i + 1;
            
            if (groupAssignment.orderDetailIds() == null || groupAssignment.orderDetailIds().isEmpty()) {
                validationErrors.add("Group " + groupNumber + ": Missing order details");
            }
            if (groupAssignment.vehicleId() == null) {
                validationErrors.add("Group " + groupNumber + ": Missing vehicle");
            }
            if (groupAssignment.driverId_1() == null) {
                validationErrors.add("Group " + groupNumber + ": Missing driver 1");
            }
            if (groupAssignment.driverId_2() == null) {
                validationErrors.add("Group " + groupNumber + ": Missing driver 2");
            }
            if (groupAssignment.routeInfo() == null || groupAssignment.routeInfo().segments() == null || groupAssignment.routeInfo().segments().isEmpty()) {
                validationErrors.add("Group " + groupNumber + ": Missing route information");
            }
            if (groupAssignment.seals() == null || groupAssignment.seals().isEmpty()) {
                validationErrors.add("Group " + groupNumber + ": Missing seals (at least 1 seal required)");
            }
        }
        
        if (!validationErrors.isEmpty()) {
            String errorMessage = "Cannot create vehicle assignments. Validation errors: " + String.join("; ", validationErrors);
            log.error(errorMessage);
            throw new NotFoundException(errorMessage, ErrorEnum.INVALID_REQUEST.getErrorCode());
        }
        
        List<VehicleAssignmentResponse> createdAssignments = new ArrayList<>();
        // Keep track of orders that need status update
        Set<UUID> orderIdsToUpdate = new HashSet<>();

        // Xử lý từng nhóm order detail
        for (OrderDetailGroupAssignment groupAssignment : request.groupAssignments()) {
            // Kiểm tra các order detail có tồn tại không
            List<UUID> orderDetailIds = groupAssignment.orderDetailIds();
            if (orderDetailIds.isEmpty()) {
                log.warn("Skipping group assignment with no order details");
                continue;
            }

            // Kiểm tra xe có tồn tại không
            VehicleEntity vehicle = vehicleEntityService.findEntityById(groupAssignment.vehicleId())
                    .orElseThrow(() -> new NotFoundException(
                            ErrorEnum.VEHICLE_NOT_FOUND.getMessage(),
                            ErrorEnum.VEHICLE_NOT_FOUND.getErrorCode()
                    ));

            // Kiểm tra xe có đang được sử dụng trong assignment khác không
            Optional<VehicleAssignmentEntity> existingAssignment =
                    entityService.findVehicleAssignmentByVehicleEntityAndStatus(
                            vehicle, CommonStatusEnum.ACTIVE.name());

            if (existingAssignment.isPresent()) {
                throw new NotFoundException(
                        "Xe " + vehicle.getLicensePlateNumber() + " đang được sử dụng trong assignment khác",
                        VEHICLE_NOT_AVAILABLE
                );
            }

            // Kiểm tra tài xế có tồn tại không
            DriverEntity driver1 = driverEntityService.findEntityById(groupAssignment.driverId_1())
                    .orElseThrow(() -> new NotFoundException(
                            "Tài xế 1 không tìm thấy với ID: " + groupAssignment.driverId_1(),
                            ErrorEnum.NOT_FOUND.getErrorCode()
                    ));

            DriverEntity driver2 = driverEntityService.findEntityById(groupAssignment.driverId_2())
                    .orElseThrow(() -> new NotFoundException(
                            "Tài xế 2 không tìm thấy với ID: " + groupAssignment.driverId_2(),
                            ErrorEnum.NOT_FOUND.getErrorCode()
                    ));

            // Kiểm tra tài xế có đang được gán trong assignment khác không
            if (entityService.existsActiveAssignmentForDriver(driver1.getId())) {
                throw new NotFoundException(
                        "Tài xế " + driver1.getUser().getFullName() + " đang được gán trong assignment khác",
                        DRIVER_NOT_AVAILABLE
                );
            }

            if (entityService.existsActiveAssignmentForDriver(driver2.getId())) {
                throw new NotFoundException(
                        "Tài xế " + driver2.getUser().getFullName() + " đang được gán trong assignment khác",
                        DRIVER_NOT_AVAILABLE
                );
            }

            // Tạo vehicle assignment mới
            VehicleAssignmentEntity assignment = new VehicleAssignmentEntity();
            assignment.setVehicleEntity(vehicle);
            assignment.setDriver1(driver1);
            assignment.setDriver2(driver2);
            assignment.setDescription(groupAssignment.description());
            assignment.setStatus(CommonStatusEnum.ACTIVE.name());
            assignment.setTrackingCode(generateCode(prefixVehicleAssignmentCode));

            // Lưu assignment
            VehicleAssignmentEntity savedAssignment = entityService.save(assignment);

            try {
                createInitialJourneyForAssignment(savedAssignment, orderDetailIds, groupAssignment.routeInfo());
            } catch (Exception e) {
                log.error("Failed to create journey history for assignment {}: {}", savedAssignment.getId(), e.getMessage());
                // continue without failing whole operation
            }

            // Tạo seal mới nếu có thông tin seal
            if (groupAssignment.seals() != null && !groupAssignment.seals().isEmpty()) {
                try {
                    // Tạo nhiều seal cho assignment
                    for (capstone_project.dtos.request.seal.SealInfo sealInfo : groupAssignment.seals()) {
                        // Validate seal data
                        if (sealInfo.sealCode() == null || sealInfo.sealCode().trim().isEmpty()) {
                            log.warn("Skipping seal with empty code for assignment {}", savedAssignment.getId());
                            continue;
                        }
                        createSealForAssignment(savedAssignment, sealInfo.sealCode(), sealInfo.description());
                    }
                } catch (Exception e) {
                    log.error("Failed to create seals for assignment {}: {}", savedAssignment.getId(), e.getMessage(), e);
                    // continue without failing the operation
                }
            }

            // Gán order details vào assignment
            for (UUID orderDetailId : orderDetailIds) {
                var orderDetail = orderDetailEntityService.findEntityById(orderDetailId)
                        .orElseThrow(() -> new NotFoundException(
                                "Order detail không tìm thấy với ID: " + orderDetailId,
                                ErrorEnum.NOT_FOUND.getErrorCode()
                        ));

                // Gán vehicle assignment cho order detail
                orderDetail.setVehicleAssignmentEntity(savedAssignment);
                orderDetailEntityService.save(orderDetail);

                // Update order detail status to ASSIGNED_TO_DRIVER
                orderDetailService.updateOrderDetailStatus(orderDetail.getId(), OrderStatusEnum.ASSIGNED_TO_DRIVER);

                // Add order ID to the set of orders that need status update
                if (orderDetail.getOrderEntity() != null) {
                    orderIdsToUpdate.add(orderDetail.getOrderEntity().getId());
                }
            }

            // Thêm vào danh sách kết quả
            createdAssignments.add(mapper.toResponse(savedAssignment));
        }

        // Update status of all affected orders to ASSIGNED_TO_DRIVER
        for (UUID orderId : orderIdsToUpdate) {
            orderService.updateOrderStatus(orderId, OrderStatusEnum.ASSIGNED_TO_DRIVER);
        }

        return createdAssignments;
    }

    private void createInitialJourneyForAssignment(VehicleAssignmentEntity assignment, List<UUID> orderDetailIds, RouteInfo routeInfo) {
        // If there's route information from the client, use that to create the journey
        if (routeInfo != null && routeInfo.segments() != null && !routeInfo.segments().isEmpty()) {
            log.info("Creating journey history with route information for assignment {}", assignment.getId());
            // Log toll information received from client
            log.info("Route info toll data: totalTollFee={}, totalTollCount={}, segments={}",
                routeInfo.totalTollFee(),
                routeInfo.totalTollCount(),
                routeInfo.segments().size());
            
            // Consolidate route segments to standard 3-segment format while preserving intermediate points
            List<RouteSegmentInfo> consolidatedSegments = consolidateRouteSegments(routeInfo.segments());
            log.info("Consolidated {} original segments into {} standard segments",
                    routeInfo.segments().size(), consolidatedSegments.size());

            // Create a new RouteInfo with consolidated segments
            RouteInfo consolidatedRouteInfo = new RouteInfo(
                consolidatedSegments,
                routeInfo.totalTollFee(),
                routeInfo.totalTollCount(),
                routeInfo.totalDistance()
            );

            // Build journey history
            capstone_project.entity.order.order.JourneyHistoryEntity journeyHistory =
                    new capstone_project.entity.order.order.JourneyHistoryEntity();
            journeyHistory.setJourneyName("Journey for " + assignment.getTrackingCode());
            journeyHistory.setJourneyType("INITIAL");
            journeyHistory.setStatus(CommonStatusEnum.ACTIVE.name());
            journeyHistory.setVehicleAssignment(assignment);

            // Set total toll fee (BigDecimal -> Long)
            Long totalTollFeeLong = null;
            if (consolidatedRouteInfo.totalTollFee() != null) {
                totalTollFeeLong = consolidatedRouteInfo.totalTollFee().setScale(0, RoundingMode.HALF_UP).longValue();
                log.info("Setting journey total toll fee: {}", totalTollFeeLong);
            } else {
                log.warn("Total toll fee is missing in the route info for assignment {}", assignment.getId());
                // Calculate total from segments as fallback
                BigDecimal calculatedTotal = BigDecimal.ZERO;
                boolean hasSegmentTolls = false;
                
                if (consolidatedRouteInfo.segments() != null) {
                    for (RouteSegmentInfo segmentInfo : consolidatedRouteInfo.segments()) {
                        if (segmentInfo.estimatedTollFee() != null) {
                            calculatedTotal = calculatedTotal.add(segmentInfo.estimatedTollFee());
                            hasSegmentTolls = true;
                        }
                    }
                }
                
                if (hasSegmentTolls) {
                    totalTollFeeLong = calculatedTotal.setScale(0, RoundingMode.HALF_UP).longValue();
                    log.info("Calculated total toll fee from segments: {}", totalTollFeeLong);
                }
            }
            journeyHistory.setTotalTollFee(totalTollFeeLong);

            // Set total toll count
            Integer totalTollCount = consolidatedRouteInfo.totalTollCount();
            if (totalTollCount == null) {
                // Calculate total toll count from segments if not provided
                totalTollCount = 0;
                for (RouteSegmentInfo segmentInfo : consolidatedRouteInfo.segments()) {
                    if (segmentInfo.tollDetails() != null) {
                        totalTollCount += segmentInfo.tollDetails().size();
                    }
                }
                log.info("Calculated total toll count from segments: {}", totalTollCount);
            }
            journeyHistory.setTotalTollCount(totalTollCount);

            // Create journey segments from route info
            List<capstone_project.entity.order.order.JourneySegmentEntity> segments = new ArrayList<>();

            for (RouteSegmentInfo segmentInfo : consolidatedRouteInfo.segments()) {
                capstone_project.entity.order.order.JourneySegmentEntity segment =
                        new capstone_project.entity.order.order.JourneySegmentEntity();

                segment.setSegmentOrder(segmentInfo.segmentOrder());
                segment.setStartPointName(segmentInfo.startPointName());
                segment.setEndPointName(segmentInfo.endPointName());
                segment.setStartLatitude(segmentInfo.startLatitude());
                segment.setStartLongitude(segmentInfo.startLongitude());
                segment.setEndLatitude(segmentInfo.endLatitude());
                segment.setEndLongitude(segmentInfo.endLongitude());

                // Convert BigDecimal -> Integer (rounding)
                segment.setDistanceMeters(segmentInfo.distanceMeters() != null
                        ? segmentInfo.distanceMeters().setScale(0, RoundingMode.HALF_UP).intValue()
                        : null);

                // Convert BigDecimal -> Long (rounding)
                Long segmentTollFee = null;
                if (segmentInfo.estimatedTollFee() != null) {
                    segmentTollFee = segmentInfo.estimatedTollFee().setScale(0, RoundingMode.HALF_UP).longValue();
                    log.info("Setting segment [{}] toll fee: {}", segmentInfo.segmentOrder(), segmentTollFee);
                } else {
                    log.warn("Segment [{}] is missing toll fee information", segmentInfo.segmentOrder());
                }
                segment.setEstimatedTollFee(segmentTollFee);
                segment.setStatus("PENDING");

                // Store path coordinates as JSON
                if (segmentInfo.pathCoordinates() != null && !segmentInfo.pathCoordinates().isEmpty()) {
                    try {
                        segment.setPathCoordinatesJson(objectMapper.writeValueAsString(segmentInfo.pathCoordinates()));
                    } catch (Exception e) {
                        log.warn("Failed to serialize path coordinates for segment {}: {}",
                            segmentInfo.segmentOrder(), e.getMessage());
                    }
                }

                // Store toll details as JSON
                if (segmentInfo.tollDetails() != null && !segmentInfo.tollDetails().isEmpty()) {
                    try {
                        segment.setTollDetailsJson(objectMapper.writeValueAsString(segmentInfo.tollDetails()));
                        log.info("Stored toll details JSON for segment [{}] with {} toll points",
                            segmentInfo.segmentOrder(), segmentInfo.tollDetails().size());
                    } catch (Exception e) {
                        log.warn("Failed to serialize toll details for segment {}: {}",
                            segmentInfo.segmentOrder(), e.getMessage());
                    }
                }

                segment.setJourneyHistory(journeyHistory);
                segments.add(segment);
            }

            journeyHistory.setJourneySegments(segments);

            // Save journey history with all segments
            journeyHistory = journeyHistoryEntityService.save(journeyHistory);

            log.info("Created journey history {} with {} segments for assignment {}, totalTollFee={}, totalTollCount={}",
                    journeyHistory.getId(), segments.size(), assignment.getId(),
                    journeyHistory.getTotalTollFee(), journeyHistory.getTotalTollCount());

            return;
        }

        // Fallback to the existing method if no route information is provided
        // Determine depot/unit coordinates - fallback to vehicle current position if configured
        BigDecimal unitLat = null;
        BigDecimal unitLng = null;
        VehicleEntity vehicle = assignment.getVehicleEntity();
        if (vehicle != null) {
            try {
                // adjust method names if your VehicleEntity uses different getters
                unitLat = vehicle.getCurrentLatitude();
                unitLng = vehicle.getCurrentLongitude();
            } catch (Exception ignored) {}
        }

        // Use first orderDetail's order addresses as representative pickup/delivery
        BigDecimal pickupLat = null;
        BigDecimal pickupLng = null;
        BigDecimal deliveryLat = null;
        BigDecimal deliveryLng = null;

        if (!orderDetailIds.isEmpty()) {
            OrderDetailEntity od = orderDetailEntityService.findEntityById(orderDetailIds.get(0)).orElse(null);
            if (od != null && od.getOrderEntity() != null) {
                OrderEntity order = od.getOrderEntity();
                AddressEntity pickupAddr = order.getPickupAddress();
                AddressEntity deliveryAddr = order.getDeliveryAddress();
                if (pickupAddr != null) {
                    // adjust getters if your AddressEntity uses different names / types
                    pickupLat = pickupAddr.getLatitude();
                    pickupLng = pickupAddr.getLongitude();
                }
                if (deliveryAddr != null) {
                    deliveryLat = deliveryAddr.getLatitude();
                    deliveryLng = deliveryAddr.getLongitude();
                }
            }
        }

        // fallback unit -> pickup/delivery if unit missing
        if ((unitLat == null || unitLng == null) && pickupLat != null && pickupLng != null) {
            unitLat = pickupLat; unitLng = pickupLng;
        } else if ((unitLat == null || unitLng == null) && deliveryLat != null && deliveryLng != null) {
            unitLat = deliveryLat; unitLng = deliveryLng;
        }

        // if any coordinate missing -> skip
        if (unitLat == null || unitLng == null || pickupLat == null || pickupLng == null || deliveryLat == null || deliveryLng == null) {
            log.warn("Missing coords for creating journey for assignment {}. unit:({},{}) pickup:({},{}) delivery:({},{})",
                    assignment.getId(), unitLat, unitLng, pickupLat, pickupLng, deliveryLat, deliveryLng);
            return;
        }

        // Build journey history
        capstone_project.entity.order.order.JourneyHistoryEntity journeyHistory = new capstone_project.entity.order.order.JourneyHistoryEntity();
        journeyHistory.setJourneyName("Journey for " + assignment.getTrackingCode());
        journeyHistory.setJourneyType("INITIAL");
        journeyHistory.setStatus(CommonStatusEnum.ACTIVE.name());
        journeyHistory.setVehicleAssignment(assignment);

        // Segment 1: unit -> pickup
        capstone_project.entity.order.order.JourneySegmentEntity s1 = new capstone_project.entity.order.order.JourneySegmentEntity();
        s1.setSegmentOrder(1);
        s1.setStartPointName("Unit");
        s1.setEndPointName("Pickup");
        s1.setStartLatitude(unitLat);
        s1.setStartLongitude(unitLng);
        s1.setEndLatitude(pickupLat);
        s1.setEndLongitude(pickupLng);
        s1.setStatus("PENDING");

        // Segment 2: pickup -> delivery
        capstone_project.entity.order.order.JourneySegmentEntity s2 = new capstone_project.entity.order.order.JourneySegmentEntity();
        s2.setSegmentOrder(2);
        s2.setStartPointName("Pickup");
        s2.setEndPointName("Delivery");
        s2.setStartLatitude(pickupLat);
        s2.setStartLongitude(pickupLng);
        s2.setEndLatitude(deliveryLat);
        s2.setEndLongitude(deliveryLng);
        s2.setStatus("PENDING");

        // Save segments
        List<capstone_project.entity.order.order.JourneySegmentEntity> segments = Arrays.asList(s1, s2);
        journeyHistory.setJourneySegments(segments);

        // Assign segments to journey history
        s1.setJourneyHistory(journeyHistory);
        s2.setJourneyHistory(journeyHistory);

        // Save journey history
        journeyHistoryEntityService.save(journeyHistory);
    }

    private void createInitialJourneyForAssignment(VehicleAssignmentEntity assignment, List<UUID> orderDetailIds) {
        // Call the overloaded method with null route info to use the default behavior
        createInitialJourneyForAssignment(assignment, orderDetailIds, null);
    }

    private String generateCode(String prefix) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String randomPart = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return prefix + timestamp + "-" + randomPart;
    }

    /**
     * Lấy thông tin chi tiết về các order detail
     * @param detailIds Danh sách ID của order details
     * @return Danh sách thông tin chi tiết về các order details
     */
    private List<GroupedVehicleAssignmentResponse.OrderDetailInfo> getOrderDetailInfos(List<UUID> detailIds) {
        List<GroupedVehicleAssignmentResponse.OrderDetailInfo> detailInfos = new ArrayList<>();

        for (UUID detailId : detailIds) {
            OrderDetailEntity orderDetail = orderDetailEntityService.findEntityById(detailId)
                    .orElseThrow(() -> new NotFoundException(
                            "Order detail not found: " + detailId,
                            ErrorEnum.NOT_FOUND.getErrorCode()
                    ));

            // Lấy weightBaseUnit từ orderDetail
            Double weightBaseUnit = null;
            if (orderDetail.getWeightBaseUnit() != null) {
                weightBaseUnit = orderDetail.getWeightBaseUnit().doubleValue();
            }

            detailInfos.add(new GroupedVehicleAssignmentResponse.OrderDetailInfo(
                    orderDetail.getId(),
                    orderDetail.getTrackingCode(),
                    weightBaseUnit,
                    orderDetail.getUnit(),
                    orderDetail.getDescription()
            ));
        }

        return detailInfos;
    }

    /**
     * Kết hợp các phần của địa chỉ thành một chuỗi hoàn chỉnh
     */
    private String combineAddress(String street, String ward, String province) {
        StringBuilder fullAddress = new StringBuilder();

        if (street != null && !street.isEmpty()) {
            fullAddress.append(street);
        }

        if (ward != null && !ward.isEmpty()) {
            if (fullAddress.length() > 0) fullAddress.append(", ");
            fullAddress.append(ward);
        }

        if (province != null && !province.isEmpty()) {
            if (fullAddress.length() > 0) fullAddress.append(", ");
            fullAddress.append(province);
        }

        return fullAddress.toString();
    }

    /**
     * Tìm xe và tài xế phù hợp cho nhóm order detail
     * @param detailIds Danh sách ID của order details
     * @param vehicleRule Quy tắc về loại xe
     * @return Danh sách gợi ý xe và tài xế phù hợp cho nhóm
     */
    private List<GroupedVehicleAssignmentResponse.VehicleSuggestionResponse> findSuitableVehiclesForGroup(
            List<UUID> detailIds, VehicleTypeRuleEntity vehicleRule, UUID vehicleTypeId) {

        List<GroupedVehicleAssignmentResponse.VehicleSuggestionResponse> vehicleSuggestions = new ArrayList<>();

        // Lấy danh sách xe phù hợp với loại xe từ rule
        VehicleTypeEnum vehicleTypeEnum = VehicleTypeEnum.valueOf(vehicleRule.getVehicleTypeEntity().getVehicleTypeName());
        List<VehicleEntity> availableVehicles = vehicleEntityService.getVehicleEntitiesByVehicleTypeEntityAndStatus(
                vehicleRule.getVehicleTypeEntity(), CommonStatusEnum.ACTIVE.name());

        // Lấy danh sách tài xế hợp lệ cho loại xe này
        List<DriverEntity> allEligibleDrivers = driverEntityService.findByStatus(CommonStatusEnum.ACTIVE.name())
                .stream()
                .filter(d -> driverService.isCheckClassDriverLicenseForVehicleType(d, vehicleTypeEnum))
                .filter(d -> !entityService.existsActiveAssignmentForDriver(d.getId()))
                .toList();

        // Sắp xếp xe theo mức độ sử dụng (ít dùng nhất lên đầu)
        List<UUID> vehicleIds = availableVehicles.stream().map(VehicleEntity::getId).toList();
        List<UUID> sortedVehicleIds = sortVehiclesByUsageThisMonth(vehicleIds);

        // Giới hạn số lượng xe gợi ý
        final int MAX_VEHICLES_PER_GROUP = 3;
        int vehicleCount = 0;

        for (UUID vehicleId : sortedVehicleIds) {
            if (vehicleCount >= MAX_VEHICLES_PER_GROUP) break;

            VehicleEntity vehicle = vehicleEntityService.findEntityById(vehicleId)
                    .orElseThrow(() -> new NotFoundException(
                            "Vehicle not found: " + vehicleId,
                            ErrorEnum.NOT_FOUND.getErrorCode()
                    ));

            Optional<VehicleAssignmentEntity> activeAssignment =
                    entityService.findVehicleAssignmentByVehicleEntityAndStatus(vehicle, CommonStatusEnum.ACTIVE.name());

            if (activeAssignment.isPresent()) continue;  // Bỏ qua xe đang có assignment

            // Tìm tài xế phù hợp cho xe này
            List<DriverEntity> preferredDrivers = findPreferredDriversForVehicle(vehicle, allEligibleDrivers, vehicleTypeEnum);

            if (!preferredDrivers.isEmpty()) {
                List<GroupedVehicleAssignmentResponse.DriverSuggestionResponse> driverSuggestions =
                        createDriverSuggestions(preferredDrivers);

                // Đánh dấu xe được đề xuất nhất (xe đầu tiên trong danh sách)
                boolean isRecommended = (vehicleCount == 0);

                UUID finalVehicleTypeId = vehicle.getVehicleTypeEntity() != null
                        ? vehicle.getVehicleTypeEntity().getId()
                        : vehicleTypeId;

                String vehicleTypeName = vehicle.getVehicleTypeEntity() != null
                        ? vehicle.getVehicleTypeEntity().getVehicleTypeName()
                        : vehicleRule.getVehicleTypeEntity().getVehicleTypeName();

                vehicleSuggestions.add(new GroupedVehicleAssignmentResponse.VehicleSuggestionResponse(
                        vehicle.getId(),
                        vehicle.getLicensePlateNumber(),
                        vehicle.getModel(),
                        vehicle.getManufacturer(),
                        finalVehicleTypeId,
                        vehicleTypeName,
                        driverSuggestions,
                        isRecommended
                ));

                vehicleCount++;
            }
        }

        return vehicleSuggestions;
    }

    /**
     * Find preferred drivers for a specific vehicle
     * Enhanced to consider recent activity and workload when selecting drivers
     */
    private List<DriverEntity> findPreferredDriversForVehicle(
            VehicleEntity vehicle, List<DriverEntity> allEligibleDrivers, VehicleTypeEnum vehicleTypeEnum) {

        List<DriverEntity> preferredDrivers = new ArrayList<>();
        final int MAX_DRIVERS_PER_VEHICLE = 4;

        // Collect last assignment drivers for this vehicle (preserve ordering)
        List<VehicleAssignmentEntity> pastAssignments = entityService.findAssignmentsByVehicleOrderByCreatedAtDesc(vehicle);
        List<UUID> lastAssignmentDriverOrder = new ArrayList<>();
        if (!pastAssignments.isEmpty()) {
            VehicleAssignmentEntity lastAssignment = pastAssignments.get(0);
            if (lastAssignment.getDriver1() != null) lastAssignmentDriverOrder.add(lastAssignment.getDriver1().getId());
            if (lastAssignment.getDriver2() != null) lastAssignmentDriverOrder.add(lastAssignment.getDriver2().getId());
        }

        // Get all driver IDs to calculate metrics
        Set<UUID> driverIds = allEligibleDrivers.stream().map(DriverEntity::getId).collect(Collectors.toSet());

        // Use our improved activity scoring system - weighted by recency
        Map<UUID, Integer> recentActivityMap = countRecentActivitiesByDrivers(driverIds, 30);

        // Count completed trips for each driver
        Map<UUID, Integer> completedTripsMap = countCompletedTripsByDrivers(driverIds);

        // Get the average number of completed trips across all drivers
        double avgCompletedTrips = completedTripsMap.values().stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0);

        // Get violation counts for all drivers
        Map<UUID, Integer> violationCountMap = new HashMap<>();
        for (DriverEntity driver : allEligibleDrivers) {
            violationCountMap.put(driver.getId(), penaltyHistoryRepository.findByDriverId(driver.getId()).size());
        }

        // Build a comprehensive scoring system for each driver
        Map<DriverEntity, Integer> driverScoreMap = new HashMap<>();
        for (DriverEntity driver : allEligibleDrivers) {
            int score = 0;
            UUID driverId = driver.getId();

            // Factor 1: License class rank (0-200 points)
            score += licenseClassRank(driver.getLicenseClass()) * 100;

            // Factor 2: Previous assignment to this vehicle - familiarity bonus (0-500 points)
            int previousAssignmentScore = lastAssignmentDriverOrder.indexOf(driverId);
            score += (previousAssignmentScore >= 0) ? previousAssignmentScore * 50 : 500;

            // Factor 3: Recent activity (weighted by our improved scoring system) (0-300 points)
            int recentActivity = recentActivityMap.getOrDefault(driverId, 0);
            score += recentActivity * 30;

            // Factor 4: Experience vs workload balance (100-300 points)
            int completedTrips = completedTripsMap.getOrDefault(driverId, 0);

            // Prefer drivers with experience but don't overwork them
            if (completedTrips < avgCompletedTrips * 0.5) {
                // Less experienced driver gets medium priority
                score += 200;
            } else if (completedTrips > avgCompletedTrips * 1.5) {
                // Overworked driver gets lower priority
                score += 300;
            } else {
                // Balanced workload gets highest priority
                score += 100;
            }

            // Factor 5: Violations (0-400 points)
            int violations = violationCountMap.getOrDefault(driverId, 0);
            score += violations * 80;

            // Factor 6: Driver workload balance over time (0-300 points)
            // Calculate days since last assignment to prevent overworking recent drivers
            Optional<VehicleAssignmentEntity> lastAssignment = entityService.findLatestAssignmentByDriverId(driverId);
            if (lastAssignment.isPresent()) {
                LocalDateTime lastAssignmentDate = lastAssignment.get().getCreatedAt();
                long daysSinceLastAssignment = java.time.Duration.between(
                        lastAssignmentDate,
                        LocalDateTime.now()
                ).toDays();

                // Drivers who haven't been assigned recently get priority
                if (daysSinceLastAssignment < 2) {
                    // Assigned very recently (less than 2 days ago)
                    score += 300;
                } else if (daysSinceLastAssignment < 5) {
                    // Assigned recently (2-5 days ago)
                    score += 200;
                } else if (daysSinceLastAssignment < 14) {
                    // Assigned not too recently (5-14 days ago)
                    score += 100;
                } else {
                    // Hasn't been assigned in a while (14+ days)
                    score += 0; // Highest priority - no penalty
                }
            }

            // Store the comprehensive score
            driverScoreMap.put(driver, score);
        }

        // Sort drivers by their comprehensive score (lower is better)
        List<DriverEntity> sortedCandidates = new ArrayList<>(allEligibleDrivers);
        sortedCandidates.sort(Comparator.comparingInt(d -> driverScoreMap.getOrDefault(d, Integer.MAX_VALUE)));

        // Log the top 5 driver scores for debugging if needed
        if (!sortedCandidates.isEmpty()) {
            int logLimit = Math.min(sortedCandidates.size(), 5);
            for (int i = 0; i < logLimit; i++) {
                DriverEntity driver = sortedCandidates.get(i);
                log.debug("Driver score {} for {}: {}", i+1, driver.getUser().getFullName(),
                        driverScoreMap.getOrDefault(driver, -1));
            }
        }

        // Select up to MAX_DRIVERS_PER_VEHICLE distinct drivers from sortedCandidates
        for (DriverEntity driver : sortedCandidates) {
            if (preferredDrivers.size() >= MAX_DRIVERS_PER_VEHICLE) break;
            if (!preferredDrivers.contains(driver)) {
                preferredDrivers.add(driver);
            }
        }

        return preferredDrivers;
    }

    private int licenseClassRank(String licenseClass) {
        if (licenseClass == null) return Integer.MAX_VALUE;
        return switch (licenseClass.trim().toUpperCase()) {
            case "B2" -> 0;
            case "C" -> 1;
            default -> 100;
        };
    }

    /**
     * Create driver suggestion DTOs from driver entities
     */
    private List<GroupedVehicleAssignmentResponse.DriverSuggestionResponse> createDriverSuggestions(List<DriverEntity> drivers) {
        // Thu thập tất cả driver ID để tính số chuyến đã hoàn thành
        Set<UUID> driverIds = drivers.stream().map(DriverEntity::getId).collect(Collectors.toSet());

        // Tính số chuyến đã hoàn thành cho mỗi tài xế
        Map<UUID, Integer> driversCompletedTripsMap = countCompletedTripsByDrivers(driverIds);

        // Tìm thời gian hoạt động gần nhất của mỗi tài xế
        Map<UUID, String> driverLastActiveTimeMap = findLastActiveTimeForDrivers(driverIds);

        // Xác định tài xế phù hợp nhất (2 tài xế đầu tiên trong danh sách)
        List<UUID> recommendedDriverIds = drivers.stream()
                .limit(2)
                .map(DriverEntity::getId)
                .toList();

        return drivers.stream().map(driver -> {
            // Tính toán số lượng vi phạm từ PenaltyHistory của tài xế
            int violationCount = penaltyHistoryRepository.findByDriverId(driver.getId()).size();

            // Lấy số chuyến đã hoàn thành từ map
            int completedTrips = driversCompletedTripsMap.getOrDefault(driver.getId(), 0);

            // Lấy thời gian hoạt động gần nhất từ map
            String lastActiveTime = driverLastActiveTimeMap.getOrDefault(driver.getId(), "Chưa có hoạt động");

            // Tính thời gian làm việc dựa trên ngày tạo hồ sơ của tài xế
            String workExperience = calculateWorkExperience(driver);

            return new GroupedVehicleAssignmentResponse.DriverSuggestionResponse(
                    driver.getId(),
                    driver.getUser().getFullName(),
                    driver.getDriverLicenseNumber(),
                    driver.getLicenseClass(),
                    recommendedDriverIds.contains(driver.getId()), // đánh dấu tài xế phù hợp nhất
                    violationCount,
                    completedTrips,
                    workExperience,
                    lastActiveTime
            );
        }).toList();
    }

    /**
     * Tính thời gian làm việc của tài xế
     */
    private String calculateWorkExperience(DriverEntity driver) {
        String workExperience = "Chưa có dữ liệu";

        try {
            if (driver.getCreatedAt() != null) {
                LocalDate joinDate = driver.getCreatedAt().toLocalDate();
                LocalDate today = LocalDate.now();

                // Tính số năm làm việc
                int yearsOfWork = today.getYear() - joinDate.getYear();

                // Điều chỉnh nếu chưa đến ngày kỷ niệm hàng năm
                if (today.getMonthValue() < joinDate.getMonthValue() ||
                        (today.getMonthValue() == joinDate.getMonthValue() &&
                                today.getDayOfMonth() < joinDate.getDayOfMonth())) {
                    yearsOfWork--;
                }

                // Tính số tháng làm việc
                int monthsOfWork = today.getMonthValue() - joinDate.getMonthValue();
                if (monthsOfWork < 0) monthsOfWork += 12;
                if (today.getDayOfMonth() < joinDate.getDayOfMonth()) {
                    monthsOfWork = (monthsOfWork + 11) % 12; // Điều chỉnh nếu chưa đến ngày kỷ niệm hàng tháng
                }

                if (yearsOfWork == 0) {
                    workExperience = monthsOfWork + " tháng";
                } else {
                    workExperience = yearsOfWork + " năm";
                }
            }
        } catch (Exception e) {
            log.error("Lỗi khi tính thời gian làm việc cho tài xế {}: {}", driver.getId(), e.getMessage());
        }

        return workExperience;
    }

    /**
     * Tạo seal mới cho vehicle assignment
     * @param assignment Vehicle assignment entity
     * @param sealCode Mã seal
     * @param sealDescription Mô tả seal
     */
    private void createSealForAssignment(VehicleAssignmentEntity assignment, String sealCode, String sealDescription) {
        log.info("Creating seal for vehicle assignment {}: sealCode={}", assignment.getId(), sealCode);

        // FIXED: Seal code không cần unique toàn hệ thống
        // Chỉ cần unique trong cùng 1 vehicle assignment
        // Cho phép tái sử dụng seal code cho các vehicle assignment khác nhau
        
        String finalSealCode = sealCode;

        // Improve description null check to also handle empty strings and whitespace
        String finalDescription = sealDescription;
        if (finalDescription == null || finalDescription.trim().isEmpty()) {
            finalDescription = "Seal for " + assignment.getTrackingCode();
        }

        SealEntity seals = SealEntity.builder()
                .sealCode(finalSealCode)
                .description(finalDescription)
                .status(SealEnum.ACTIVE.name())
                .vehicleAssignment(assignment)
                .build();

        SealEntity savedSeal = sealEntityService.save(seals);
        log.info("Created order seal with ID: {} and code: {}, linked to vehicle assignment: {}",
                savedSeal.getId(), savedSeal.getSealCode(), assignment.getId());
    }

    /**
     * Consolidates route segments into the standard 3-segment format (carrier→pickup, pickup→delivery, delivery→carrier)
     * while preserving all intermediate points in the path coordinates.
     *
     * @param originalSegments The original list of route segments from the client
     * @return A consolidated list with exactly 3 segments that includes all the intermediate points
     */
    private List<RouteSegmentInfo> consolidateRouteSegments(List<RouteSegmentInfo> originalSegments) {
        if (originalSegments == null || originalSegments.isEmpty()) {
            return originalSegments;
        }

        log.info("Consolidating {} route segments into standard 3-segment format", originalSegments.size());

        // Sort segments by point names to ensure proper sequence (Carrier→Pickup→Delivery→Carrier)
        List<RouteSegmentInfo> sortedSegments = new ArrayList<>(originalSegments);
        sortedSegments.sort((s1, s2) -> {
            // Define the order of transitions
            String[] pointOrder = {"Carrier", "Pickup", "Delivery", "Carrier"};
            
            // Find position of s1's end point
            int s1EndPos = -1;
            for (int i = 0; i < pointOrder.length - 1; i++) {
                if (pointOrder[i].equals(s1.startPointName()) && pointOrder[i + 1].equals(s1.endPointName())) {
                    s1EndPos = i;
                    break;
                }
            }
            
            // Find position of s2's end point
            int s2EndPos = -1;
            for (int i = 0; i < pointOrder.length - 1; i++) {
                if (pointOrder[i].equals(s2.startPointName()) && pointOrder[i + 1].equals(s2.endPointName())) {
                    s2EndPos = i;
                    break;
                }
            }
            
            // If we can't determine position by point names, use segmentOrder as fallback
            if (s1EndPos == -1 || s2EndPos == -1) {
                return Integer.compare(s1.segmentOrder() != null ? s1.segmentOrder() : 0,
                                      s2.segmentOrder() != null ? s2.segmentOrder() : 0);
            }
            
            return Integer.compare(s1EndPos, s2EndPos);
        });

        log.info("Sorted segments: {}", sortedSegments.stream()
            .map(s -> s.startPointName() + "->" + s.endPointName())
            .toList());

        // Identify key segments by their point names
        String carrierPointName = "Carrier";
        String pickupPointName = "Pickup";
        String deliveryPointName = "Delivery";

        // Find the indices of key segments or transitions
        int firstPickupIndex = -1;
        int firstDeliveryIndex = -1;
        int lastDeliveryIndex = -1;

        for (int i = 0; i < sortedSegments.size(); i++) {
            RouteSegmentInfo segment = sortedSegments.get(i);

            // Find first segment that ends at Pickup
            if (firstPickupIndex == -1 && pickupPointName.equals(segment.endPointName())) {
                firstPickupIndex = i;
            }

            // Find first segment that ends at Delivery
            if (firstDeliveryIndex == -1 && deliveryPointName.equals(segment.endPointName())) {
                firstDeliveryIndex = i;
            }

            // Keep track of last segment that starts from Delivery
            if (deliveryPointName.equals(segment.startPointName())) {
                lastDeliveryIndex = i;
            }
        }

        // Validate that we found all key transitions
        if (firstPickupIndex == -1 || firstDeliveryIndex == -1 || lastDeliveryIndex == -1) {
            log.warn("Could not identify all key segments in the route. firstPickupIndex={}, firstDeliveryIndex={}, lastDeliveryIndex={}",
                firstPickupIndex, firstDeliveryIndex, lastDeliveryIndex);
            return originalSegments;
        }

        // Create consolidated segments
        List<RouteSegmentInfo> consolidatedSegments = new ArrayList<>(3);

        // 1. Carrier → Pickup (with all intermediate points)
        consolidatedSegments.add(createConsolidatedSegment(
            sortedSegments.subList(0, firstPickupIndex + 1),
            1,
            carrierPointName,
            pickupPointName
        ));

        // 2. Pickup → Delivery (with all intermediate points)
        consolidatedSegments.add(createConsolidatedSegment(
            sortedSegments.subList(firstPickupIndex + 1, firstDeliveryIndex + 1),
            2,
            pickupPointName,
            deliveryPointName
        ));

        // 3. Delivery → Carrier (with all intermediate points)
        consolidatedSegments.add(createConsolidatedSegment(
            sortedSegments.subList(lastDeliveryIndex, sortedSegments.size()),
            3,
            deliveryPointName,
            carrierPointName
        ));

        log.info("Successfully consolidated segments into {} standard segments", consolidatedSegments.size());
        return consolidatedSegments;
    }

    /**
     * Creates a consolidated segment from multiple segments while preserving path coordinates
     *
     * @param segments Segments to consolidate
     * @param segmentOrder New segment order
     * @param startPointName Name of the start point
     * @param endPointName Name of the end point
     * @return Consolidated segment
     */
    private RouteSegmentInfo createConsolidatedSegment(
            List<RouteSegmentInfo> segments,
            int segmentOrder,
            String startPointName,
            String endPointName) {

        if (segments.isEmpty()) {
            throw new IllegalArgumentException("Cannot create consolidated segment from empty list");
        }

        // Use first segment for start coordinates and last segment for end coordinates
        RouteSegmentInfo firstSegment = segments.get(0);
        RouteSegmentInfo lastSegment = segments.get(segments.size() - 1);

        // Combine path coordinates from all segments
        List<List<BigDecimal>> combinedPath = new ArrayList<>();
        BigDecimal totalDistance = BigDecimal.ZERO;
        BigDecimal totalTollFee = BigDecimal.ZERO;
        List<TollDetail> combinedTollDetails = new ArrayList<>();

        for (int i = 0; i < segments.size(); i++) {
            RouteSegmentInfo segment = segments.get(i);

            // Add distance
            if (segment.distanceMeters() != null) {
                totalDistance = totalDistance.add(segment.distanceMeters());
            }

            // Add toll fee
            if (segment.estimatedTollFee() != null) {
                totalTollFee = totalTollFee.add(segment.estimatedTollFee());
            }

            // Combine toll details
            if (segment.tollDetails() != null) {
                combinedTollDetails.addAll(segment.tollDetails());
            }

            // Combine path coordinates, avoiding duplicate points at segment transitions
            if (segment.pathCoordinates() != null && !segment.pathCoordinates().isEmpty()) {
                if (combinedPath.isEmpty()) {
                    // First segment - add all points
                    combinedPath.addAll(segment.pathCoordinates());
                } else {
                    // Skip the first point of subsequent segments to avoid duplication
                    combinedPath.addAll(segment.pathCoordinates().subList(1, segment.pathCoordinates().size()));
                }
            }
        }

        // Create new consolidated segment
        return new RouteSegmentInfo(
            segmentOrder,
            startPointName,
            endPointName,
            firstSegment.startLatitude(),
            firstSegment.startLongitude(),
            lastSegment.endLatitude(),
            lastSegment.endLongitude(),
            totalDistance,
            combinedPath,
            totalTollFee,
            combinedTollDetails,
            null // rawResponse isn't needed for consolidated segment
        );
    }

    /**
     * Count recent activities for drivers within a specific time period
     * Enhanced to consider different activities with varying weights
     *
     * @param driverIds IDs of drivers to check
     * @param days Number of days to look back
     * @return Map of driver IDs to activity scores
     */
    private Map<UUID, Integer> countRecentActivitiesByDrivers(Set<UUID> driverIds, int days) {
        Map<UUID, Integer> result = new HashMap<>();
        if (driverIds.isEmpty()) {
            return result;
        }

        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);

        // Create tiers of activity dates to weight more recent activities more heavily
        LocalDateTime veryRecentDate = LocalDateTime.now().minusDays(3);  // Last 3 days (high weight)
        LocalDateTime recentDate = LocalDateTime.now().minusDays(7);      // Last week (medium weight)

        for (UUID driverId : driverIds) {
            // Get assignments with dates for this driver
            List<VehicleAssignmentEntity> recentAssignments = entityService.findAssignmentsForDriverSince(driverId, cutoffDate);

            int activityScore = 0;

            // Calculate weighted score based on recency
            for (VehicleAssignmentEntity assignment : recentAssignments) {
                LocalDateTime assignmentDate = assignment.getCreatedAt();

                // Very recent activities get higher weight
                if (assignmentDate.isAfter(veryRecentDate)) {
                    activityScore += 5;  // High weight for very recent assignments
                }
                // Recent activities get medium weight
                else if (assignmentDate.isAfter(recentDate)) {
                    activityScore += 3;  // Medium weight for assignments in the last week
                }
                // Older activities get lower weight
                else {
                    activityScore += 1;  // Low weight for older assignments
                }

                // Add extra weight if the driver was the primary driver
                if (driverId.equals(assignment.getDriver1() != null ? assignment.getDriver1().getId() : null)) {
                    activityScore += 1;  // Extra weight if they were the primary driver
                }
            }

            result.put(driverId, activityScore);
        }

        return result;
    }
}
