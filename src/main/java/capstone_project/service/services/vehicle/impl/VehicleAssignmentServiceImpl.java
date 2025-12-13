package capstone_project.service.services.vehicle.impl;

import capstone_project.common.enums.*;
import capstone_project.common.exceptions.dto.BadRequestException;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.request.vehicle.*;
import capstone_project.dtos.response.order.ListContractRuleAssignResult;
import capstone_project.dtos.response.order.contract.ContractRuleAssignResponse;
import capstone_project.dtos.response.user.DriverResponse;
import capstone_project.dtos.response.vehicle.*;
import capstone_project.entity.order.contract.ContractEntity;
import capstone_project.entity.order.order.*;
import capstone_project.entity.pricing.SizeRuleEntity;
import capstone_project.entity.user.address.AddressEntity;
import capstone_project.entity.user.driver.DriverEntity;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.entity.vehicle.VehicleEntity;
import capstone_project.repository.entityServices.order.contract.ContractEntityService;
import capstone_project.repository.entityServices.order.order.JourneyHistoryEntityService;
import capstone_project.repository.entityServices.order.order.OrderDetailEntityService;
import capstone_project.repository.entityServices.order.order.OrderEntityService;
import capstone_project.repository.entityServices.order.order.SealEntityService;
import capstone_project.repository.entityServices.pricing.SizeRuleEntityService;
import capstone_project.repository.entityServices.user.DriverEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleAssignmentEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleReservationEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleTypeEntityService;
import capstone_project.service.services.vehicle.VehicleReservationService;
import capstone_project.repository.repositories.user.PenaltyHistoryRepository;
import capstone_project.service.mapper.order.StaffOrderMapper;
import capstone_project.service.mapper.user.DriverMapper;
import capstone_project.service.mapper.vehicle.VehicleAssignmentMapper;
import capstone_project.service.mapper.vehicle.VehicleMapper;
import capstone_project.dtos.response.order.StaffVehicleAssignmentFullResponse;
import capstone_project.service.services.order.order.*;
import capstone_project.service.services.setting.ContractSettingService;
import capstone_project.service.services.thirdPartyServices.Vietmap.VietmapService;
import capstone_project.service.services.user.DriverService;
import capstone_project.service.services.vehicle.VehicleAssignmentService;
import capstone_project.service.services.notification.NotificationService;
import capstone_project.service.services.notification.NotificationBuilder;
import capstone_project.service.services.email.EmailNotificationService;
import capstone_project.dtos.request.notification.CreateNotificationRequest;
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
    private final capstone_project.repository.entityServices.device.DeviceEntityService deviceEntityService;
    private final OrderEntityService orderEntityService;
    private final OrderDetailEntityService orderDetailEntityService;
    private final ContractEntityService contractEntityService;
    private final NotificationService notificationService;
    private final ContractRuleService contractRuleService;
    private final SizeRuleEntityService sizeRuleEntityService;
    private final DriverService driverService;
    private final VehicleAssignmentMapper mapper;
    private final VehicleMapper vehicleMapper;
    private final DriverMapper driverMapper;
    private final StaffOrderMapper staffOrderMapper;
    private final PenaltyHistoryRepository penaltyHistoryRepository;
    private final ContractService contractService;
    private final OrderService orderService;
    private final OrderDetailService orderDetailService;
    private final OrderDetailStatusService orderDetailStatusService;
    private final JourneyHistoryEntityService journeyHistoryEntityService;
    private final VietmapService vietmapService;
    private final SealEntityService sealEntityService;
    private final ContractSettingService contractSettingService;
    private final VehicleReservationEntityService vehicleReservationEntityService;
    private final VehicleReservationService vehicleReservationService;
    private final EmailNotificationService emailNotificationService;

    private final ObjectMapper objectMapper;

    @Value("${prefix.vehicle.assignment.code}")
    private String prefixVehicleAssignmentCode;

    @Value("${vietmap.api.key}")
    private String mapApiKey;

    /**
     * Define custom error codes for vehicle and driver availability
     * These constants are missing from ErrorEnum
     */
    private static final long VEHICLE_NOT_AVAILABLE = 30;
    private static final long DRIVER_NOT_AVAILABLE = 31;

    /**
     * Get effective contract value - prioritize adjustedValue if > 0, otherwise use totalValue
     * This ensures notifications show the correct payment amounts
     */
    private double getEffectiveContractValue(ContractEntity contract) {
        if (contract.getAdjustedValue() != null && contract.getAdjustedValue().doubleValue() > 0) {
            return contract.getAdjustedValue().doubleValue();
        }
        return contract.getTotalValue() != null ? contract.getTotalValue().doubleValue() : 0.0;
    }

    @Override
    public List<VehicleAssignmentResponse> getAllAssignments() {
        
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
        
        VehicleAssignmentEntity entity = entityService.findEntityById(id)
                .orElseThrow(() -> new NotFoundException(
                        "Assignment is not found with ASSIGNMENT ID: " + id,
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));
        return mapper.toResponse(entity);
    }

    @Override
    public StaffVehicleAssignmentFullResponse getFullAssignmentById(UUID id) {
        VehicleAssignmentEntity entity = entityService.findEntityById(id)
                .orElseThrow(() -> new NotFoundException(
                        "Assignment is not found with ASSIGNMENT ID: " + id,
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));
        
        // Use StaffOrderMapper to build full response with order info and order details
        VehicleAssignmentResponse basicResponse = mapper.toResponse(entity);
        return staffOrderMapper.toStaffVehicleAssignmentFullResponse(entity, basicResponse);
    }

    @Override
    public VehicleAssignmentResponse createAssignment(VehicleAssignmentRequest req) {

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

        orderEntityService.findEntityById(orderID)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        // Use optimized query with JOIN FETCH to prevent LazyInitializationException in WebSocket
        List<VehicleAssignmentEntity> entity = entityService.findVehicleAssignmentsWithOrderIDOptimized(orderID);
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
            UUID sizeRuleId = response.getSizeRuleId();
            SizeRuleEntity sizeRule = sizeRuleEntityService.findEntityById(sizeRuleId)
                    .orElseThrow(() -> new NotFoundException(
                            "Vehicle rule not found: " + sizeRuleId,
                            ErrorEnum.NOT_FOUND.getErrorCode()
                    ));
            VehicleTypeEnum vehicleTypeEnum = VehicleTypeEnum.valueOf(sizeRule.getVehicleTypeEntity().getVehicleTypeName());
            List<VehicleEntity> getVehiclesByVehicleType = vehicleEntityService.getVehicleEntitiesByVehicleTypeEntityAndStatus(sizeRule.getVehicleTypeEntity(), CommonStatusEnum.ACTIVE.name());

            // Lấy tất cả các tài xế hợp lệ cho loại xe này
            // Loại trừ tài xế có bằng lái hết hạn
            List<DriverEntity> allEligibleDrivers = driverEntityService.findByStatus(CommonStatusEnum.ACTIVE.name())
                    .stream()
                    .filter(d -> !driverService.isLicenseExpired(d)) // Loại trừ bằng lái hết hạn
                    .filter(d -> driverService.isCheckClassDriverLicenseForVehicleType(d, vehicleTypeEnum))
                    .filter(d -> !entityService.existsActiveAssignmentForDriver(d.getId()))
                    .toList();

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
                            !driverService.isLicenseExpired(driver1) &&
                            driverService.isCheckClassDriverLicenseForVehicleType(driver1, vehicleTypeEnum) &&
                            !entityService.existsActiveAssignmentForDriver(driver1.getId())) {
                        preferredDrivers.add(driver1);
                    }

                    if (driver2 != null && CommonStatusEnum.ACTIVE.name().equals(driver2.getStatus()) &&
                            !driverService.isLicenseExpired(driver2) &&
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
            
        } catch (Exception e) {
            log.warn("[getGroupedSuggestionsForOrder] Optimal assignment failed for orderId={}, reason={}, fallback to realistic",
                    orderID, e.getMessage());
        }

        try {
            realisticAssignments = contractService.assignVehiclesWithAvailability(orderID);
            
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

        // B3: Determine tripDate from first OrderDetail's estimatedStartTime
        LocalDate tripDate = null;
        List<OrderDetailEntity> orderDetails = orderDetailEntityService.findOrderDetailEntitiesByOrderEntityId(orderID);
        if (!orderDetails.isEmpty()) {
            OrderDetailEntity firstDetail = orderDetails.get(0);
            if (firstDetail.getEstimatedStartTime() != null) {
                tripDate = firstDetail.getEstimatedStartTime().toLocalDate();
            }
        }
        if (tripDate == null) {
            log.warn("[getGroupedSuggestionsForOrder] No estimatedStartTime found for order {}, using current date", orderID);
            tripDate = LocalDate.now();
        }

        // Chuyển đổi kết quả từ assignVehicles thành định dạng OrderDetailGroup
        // B8-B9: Pass orderId to check vehicle reservations
        List<GroupedVehicleAssignmentResponse.OrderDetailGroup> groups =
                convertAssignmentsToGroups(vehicleAssignments, tripDate, orderID);

        long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;

        return new GroupedVehicleAssignmentResponse(groups);
    }

    /**
     * Chuyển đổi kết quả từ assignVehicles sang định dạng OrderDetailGroup
     * Enhanced to track and exclude already suggested resources across groups
     * B3: Added tripDate parameter to filter drivers by date (1 driver per trip per day)
     * B8-B9: Added orderId parameter to check vehicle reservations
     */
    private List<GroupedVehicleAssignmentResponse.OrderDetailGroup> convertAssignmentsToGroups(
            List<ContractRuleAssignResponse> assignments, LocalDate tripDate, UUID orderId) {

        List<GroupedVehicleAssignmentResponse.OrderDetailGroup> groups = new ArrayList<>();
        
        // Track used resources across groups to avoid duplicate suggestions
        Set<UUID> usedVehicleIds = new HashSet<>();
        Set<UUID> usedDriverIds = new HashSet<>();

        for (ContractRuleAssignResponse assignment : assignments) {
            // Lấy thông tin về vehicle rule
            UUID sizeRuleId = assignment.getSizeRuleId();
            SizeRuleEntity sizeRule = sizeRuleEntityService.findEntityById(sizeRuleId)
                    .orElseThrow(() -> new NotFoundException(
                            "Vehicle rule not found: " + sizeRuleId,
                            ErrorEnum.NOT_FOUND.getErrorCode()
                    ));

            List<UUID> detailIds = assignment.getAssignedDetails().stream()
                    .map(detail -> UUID.fromString(detail.id()))
                    .toList();

            if (detailIds.isEmpty()) continue;

            List<GroupedVehicleAssignmentResponse.OrderDetailInfo> detailInfos =
                    getOrderDetailInfos(detailIds);

            // Pass excluded IDs to avoid duplicate suggestions
            // B3: Pass tripDate to filter drivers by date (1 driver per trip per day)
            // B8-B9: Pass orderId to check vehicle reservations
            List<GroupedVehicleAssignmentResponse.VehicleSuggestionResponse> vehicleSuggestions =
                    findSuitableVehiclesForGroup(
                            detailIds, 
                            sizeRule, 
                            sizeRule.getVehicleTypeEntity() != null ? sizeRule.getVehicleTypeEntity().getId() : null,
                            usedVehicleIds,
                            usedDriverIds,
                            tripDate,
                            orderId
                    );
            
            // Collect used resources from top recommendation to exclude from next groups
            if (!vehicleSuggestions.isEmpty()) {
                GroupedVehicleAssignmentResponse.VehicleSuggestionResponse topVehicle = vehicleSuggestions.get(0);
                
                // Mark vehicle as used
                usedVehicleIds.add(topVehicle.id());
                
                // Mark top 2 recommended drivers as used
                topVehicle.suggestedDrivers().stream()
                    .filter(GroupedVehicleAssignmentResponse.DriverSuggestionResponse::isRecommended)
                    .limit(2)
                    .forEach(d -> usedDriverIds.add(d.id()));

            }

            BigDecimal totalWeight = detailIds.stream()
                    .map(id -> orderDetailEntityService.findEntityById(id).orElse(null))
                    .filter(Objects::nonNull)
                    .map(OrderDetailEntity::getWeightTons)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Xác định lý do nhóm
            String groupingReason;
            if (detailIds.size() == 1) {
                groupingReason = "Đơn hàng không thể gộp do kích thước hoặc trọng lượng lớn";
            } else {
                groupingReason = String.format(
                        "Các đơn hàng được gộp tối ưu cho xe %s (%.1f/%.1f kg - %.1f%%)",
                        sizeRule.getSizeRuleName(),
                        totalWeight.doubleValue(),
                        sizeRule.getMaxWeight().doubleValue(),
                        totalWeight.doubleValue() * 100 / sizeRule.getMaxWeight().doubleValue()
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
     * 
     * B10: Enhanced logging for debugging and monitoring
     */
    @Override
    public List<VehicleAssignmentResponse> createGroupedAssignments(GroupedAssignmentRequest request) {
        final long startTime = System.currentTimeMillis();
        log.info("🚀 [createGroupedAssignments] Starting with {} groups", request.groupAssignments().size());

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
        
        // ============ ENHANCED VALIDATION (B2) ============
        
        // B2.1: Check for duplicate orderDetailIds across all groups
        Set<UUID> allDetailIds = new HashSet<>();
        for (OrderDetailGroupAssignment groupAssignment : request.groupAssignments()) {
            for (UUID detailId : groupAssignment.orderDetailIds()) {
                if (!allDetailIds.add(detailId)) {
                    throw new BadRequestException(
                            "Order detail " + detailId + " được khai báo ở nhiều nhóm khác nhau. Mỗi kiện hàng chỉ được gán vào 1 nhóm duy nhất.",
                            ErrorEnum.INVALID_REQUEST.getErrorCode()
                    );
                }
            }
        }
        
        // B2.2: Check all orderDetails belong to the same order
        UUID mainOrderId = null;
        OrderEntity mainOrder = null;
        LocalDate tripDate = null;
        
        for (UUID detailId : allDetailIds) {
            OrderDetailEntity detail = orderDetailEntityService.findEntityById(detailId)
                    .orElseThrow(() -> new NotFoundException(
                            "Order detail không tìm thấy với ID: " + detailId,
                            ErrorEnum.NOT_FOUND.getErrorCode()
                    ));
            
            if (mainOrderId == null) {
                mainOrderId = detail.getOrderEntity().getId();
                mainOrder = detail.getOrderEntity();
                
                // B2.3: Determine tripDate from first orderDetail's estimatedStartTime
                if (detail.getEstimatedStartTime() != null) {
                    tripDate = detail.getEstimatedStartTime().toLocalDate();
                } else {
                    log.warn("OrderDetail {} has no estimatedStartTime, using current date as tripDate", detailId);
                    tripDate = LocalDate.now();
                }
            } else {
                // Ensure all details belong to the same order
                if (!detail.getOrderEntity().getId().equals(mainOrderId)) {
                    throw new BadRequestException(
                            "Các kiện hàng thuộc nhiều đơn hàng khác nhau (" + mainOrderId + " và " + detail.getOrderEntity().getId() + "). " +
                            "Không được gộp kiện hàng từ nhiều đơn hàng vào cùng một request phân công.",
                            ErrorEnum.INVALID_REQUEST.getErrorCode()
                    );
                }
            }
            
            // B2.4: Check orderDetail status is valid (ON_PLANNING)
            if (!OrderDetailStatusEnum.ON_PLANNING.name().equals(detail.getStatus())) {
                throw new BadRequestException(
                        "Order detail " + detailId + " có trạng thái '" + detail.getStatus() + "', không phải ON_PLANNING. " +
                        "Chỉ có thể phân công xe cho kiện hàng đang ở trạng thái ON_PLANNING.",
                        ErrorEnum.INVALID_REQUEST.getErrorCode()
                );
            }
            
            // B2.5: Check orderDetail doesn't already have a vehicleAssignment
            if (detail.getVehicleAssignmentEntity() != null) {
                throw new BadRequestException(
                        "Order detail " + detailId + " đã được gán vào chuyến xe khác (Assignment ID: " + 
                        detail.getVehicleAssignmentEntity().getId() + "). Không thể gán lại.",
                        ErrorEnum.INVALID_REQUEST.getErrorCode()
                );
            }
        }
        
        // B2.6: Check Order status is ON_PLANNING
        if (mainOrder != null && !OrderStatusEnum.ON_PLANNING.name().equals(mainOrder.getStatus())) {
            throw new BadRequestException(
                    "Đơn hàng " + mainOrderId + " có trạng thái '" + mainOrder.getStatus() + "', không phải ON_PLANNING. " +
                    "Chỉ có thể phân công xe cho đơn hàng đang ở trạng thái ON_PLANNING.",
                    ErrorEnum.INVALID_REQUEST.getErrorCode()
            );
        }
        
        // B2.7: Check no duplicate vehicleId across groups (1 vehicle per group per order)
        Set<UUID> usedVehicleIdsInRequest = new HashSet<>();
        for (OrderDetailGroupAssignment groupAssignment : request.groupAssignments()) {
            UUID vehicleId = groupAssignment.vehicleId();
            if (!usedVehicleIdsInRequest.add(vehicleId)) {
                throw new BadRequestException(
                        "Xe " + vehicleId + " được sử dụng cho nhiều nhóm trong cùng request. " +
                        "Mỗi xe chỉ được gán cho 1 nhóm duy nhất trong cùng đơn hàng.",
                        ErrorEnum.INVALID_REQUEST.getErrorCode()
                );
            }
        }
        
        // B2.8: Check 1 driver per trip per day (hard constraint)
        final LocalDate finalTripDate = tripDate;
        for (OrderDetailGroupAssignment groupAssignment : request.groupAssignments()) {
            UUID driver1Id = groupAssignment.driverId_1();
            UUID driver2Id = groupAssignment.driverId_2();
            
            // Check driver1
            if (entityService.existsAssignmentForDriverOnDate(driver1Id, finalTripDate)) {
                DriverEntity driver1 = driverEntityService.findEntityById(driver1Id).orElse(null);
                String driverName = driver1 != null && driver1.getUser() != null ? driver1.getUser().getFullName() : driver1Id.toString();
                throw new BadRequestException(
                        "Tài xế " + driverName + " đã có chuyến xe trong ngày " + finalTripDate + ". " +
                        "Mỗi tài xế chỉ được phân công 1 chuyến trong 1 ngày.",
                        ErrorEnum.DRIVER_NOT_AVAILABLE.getErrorCode()
                );
            }
            
            // Check driver2
            if (entityService.existsAssignmentForDriverOnDate(driver2Id, finalTripDate)) {
                DriverEntity driver2 = driverEntityService.findEntityById(driver2Id).orElse(null);
                String driverName = driver2 != null && driver2.getUser() != null ? driver2.getUser().getFullName() : driver2Id.toString();
                throw new BadRequestException(
                        "Tài xế " + driverName + " đã có chuyến xe trong ngày " + finalTripDate + ". " +
                        "Mỗi tài xế chỉ được phân công 1 chuyến trong 1 ngày.",
                        ErrorEnum.DRIVER_NOT_AVAILABLE.getErrorCode()
                );
            }
        }
        
        log.info("✅ Enhanced validation passed for {} groups, order={}, tripDate={}", 
                request.groupAssignments().size(), mainOrderId, tripDate);
        
        // ============ END ENHANCED VALIDATION ============
        
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

            // Kiểm tra xe có hết hạn đăng kiểm không
            LocalDate today = LocalDate.now();
            if (vehicle.getInspectionExpiryDate() != null && vehicle.getInspectionExpiryDate().isBefore(today)) {
                throw new BadRequestException(
                        "Xe " + vehicle.getLicensePlateNumber() + " đã hết hạn đăng kiểm ngày " + 
                        vehicle.getInspectionExpiryDate() + ". Vui lòng gia hạn đăng kiểm trước khi phân công.",
                        VEHICLE_NOT_AVAILABLE
                );
            }

            // Kiểm tra xe có hết hạn bảo hiểm không
            if (vehicle.getInsuranceExpiryDate() != null && vehicle.getInsuranceExpiryDate().isBefore(today)) {
                throw new BadRequestException(
                        "Xe " + vehicle.getLicensePlateNumber() + " đã hết hạn bảo hiểm ngày " + 
                        vehicle.getInsuranceExpiryDate() + ". Vui lòng gia hạn bảo hiểm trước khi phân công.",
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

            // Query all devices attached to this vehicle and save device IDs
            List<capstone_project.entity.device.DeviceEntity> devices = deviceEntityService.findByVehicleId(vehicle.getId());
            if (!devices.isEmpty()) {
                String deviceIds = devices.stream()
                        .map(d -> d.getId().toString())
                        .collect(Collectors.joining(","));
                assignment.setDeviceIds(deviceIds);
                log.info("✅ [createGroupedAssignments] Found {} devices for vehicle {}: {}", 
                        devices.size(), vehicle.getLicensePlateNumber(), deviceIds);
            } else {
                log.warn("⚠️ [createGroupedAssignments] No devices found for vehicle {}", vehicle.getLicensePlateNumber());
            }
            
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

                // B5: Update order detail status using correct enum (OrderDetailStatusEnum)
                // Use OrderDetailStatusService instead of OrderDetailService for proper multi-trip handling
                orderDetailStatusService.updateOrderDetailStatus(orderDetail.getId(), OrderDetailStatusEnum.ASSIGNED_TO_DRIVER);

                // Add order ID to the set of orders that need status update
                if (orderDetail.getOrderEntity() != null) {
                    orderIdsToUpdate.add(orderDetail.getOrderEntity().getId());
                }
            }

            // Create notifications for assignment
            createAssignmentNotifications(savedAssignment, orderDetailIds);
            
            // Thêm vào danh sách kết quả
            createdAssignments.add(mapper.toResponse(savedAssignment));
        }

        // B5: Use aggregator service to update Order status based on all OrderDetails
        // This ensures proper multi-trip status calculation with priority logic
        for (UUID orderId : orderIdsToUpdate) {
            // Trigger Order status aggregation from all OrderDetails (multi-trip compliant)
            orderDetailStatusService.triggerOrderStatusUpdate(orderId);
            
            // B8-B9: Consume any existing reservations for this order
            // This marks reservations as CONSUMED since assignment is now created
            try {
                vehicleReservationService.consumeReservationsForOrder(orderId);
            } catch (Exception e) {
                log.warn("⚠️ Failed to consume reservations for order {}: {}", orderId, e.getMessage());
                // Don't throw - reservation consumption failure shouldn't break assignment process
            }
            
            // Handle full payment deadline when driver is assigned
            try {
                var contract = contractEntityService.getContractByOrderId(orderId).orElse(null);
                if (contract != null) {
                    // Set full payment deadline to 24 hours from now when driver is assigned
                    contract.setFullPaymentDeadline(LocalDateTime.now().plusHours(24));
                    contractEntityService.save(contract);
                    log.info("✅ Updated full payment deadline to +24h for order {} when driver assigned", orderId);
                }
            } catch (Exception e) {
                log.error("❌ Failed to update full payment deadline for order {}: {}", orderId, e.getMessage());
                // Don't throw - deadline update failure shouldn't break assignment process
            }
        }

        // B10: Log completion summary
        long elapsedMs = System.currentTimeMillis() - startTime;
        log.info("✅ [createGroupedAssignments] Completed in {}ms - Created {} assignments for {} orders", 
                elapsedMs, createdAssignments.size(), orderIdsToUpdate.size());

        return createdAssignments;
    }

    private void createInitialJourneyForAssignment(VehicleAssignmentEntity assignment, List<UUID> orderDetailIds, RouteInfo routeInfo) {
        // If there's route information from the client, use that to create the journey
        if (routeInfo != null && routeInfo.segments() != null && !routeInfo.segments().isEmpty()) {
            
            // Log toll information received from client

            // Consolidate route segments to standard 3-segment format while preserving intermediate points
            List<RouteSegmentInfo> consolidatedSegments = consolidateRouteSegments(routeInfo.segments());

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

                // Set distance kilometers as BigDecimal (with 2 decimal places)
                segment.setDistanceKilometers(segmentInfo.distanceKilometers() != null
                        ? segmentInfo.distanceKilometers().setScale(2, RoundingMode.HALF_UP)
                        : null);

                // Convert BigDecimal -> Long (rounding)
                Long segmentTollFee = null;
                if (segmentInfo.estimatedTollFee() != null) {
                    segmentTollFee = segmentInfo.estimatedTollFee().setScale(0, RoundingMode.HALF_UP).longValue();
                    
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
     * Enhanced to exclude already suggested resources from other groups
     * 
     * @param detailIds Danh sách ID của order details
     * @param sizeRule Quy tắc về loại xe
     * @param vehicleTypeId ID của loại xe
     * @param excludedVehicleIds Set of vehicle IDs to exclude (already suggested for other groups)
     * @param excludedDriverIds Set of driver IDs to exclude (already suggested for other groups)
     * @param tripDate Ngày chuyến để filter driver (B3: 1 driver per trip per day)
     * @param orderId Order ID để check reservation (B8-B9: exclude reservation của chính order này)
     * @return Danh sách gợi ý xe và tài xế phù hợp cho nhóm
     */
    private List<GroupedVehicleAssignmentResponse.VehicleSuggestionResponse> findSuitableVehiclesForGroup(
            List<UUID> detailIds, 
            SizeRuleEntity sizeRule, 
            UUID vehicleTypeId,
            Set<UUID> excludedVehicleIds,
            Set<UUID> excludedDriverIds,
            LocalDate tripDate,
            UUID orderId) {

        List<GroupedVehicleAssignmentResponse.VehicleSuggestionResponse> vehicleSuggestions = new ArrayList<>();

        // B8-B9: Final variables for lambda
        final LocalDate finalTripDateForReservation = tripDate;
        final UUID finalOrderId = orderId;
        final LocalDate today = LocalDate.now();

        // Lấy danh sách xe phù hợp với loại xe từ rule
        VehicleTypeEnum vehicleTypeEnum = VehicleTypeEnum.valueOf(sizeRule.getVehicleTypeEntity().getVehicleTypeName());
        List<VehicleEntity> availableVehicles = vehicleEntityService.getVehicleEntitiesByVehicleTypeEntityAndStatus(
                sizeRule.getVehicleTypeEntity(), CommonStatusEnum.ACTIVE.name())
                .stream()
                .filter(v -> !excludedVehicleIds.contains(v.getId()))  // Exclude already suggested vehicles
                // B8-B9: Exclude vehicles with RESERVED reservation on tripDate (except for this order)
                .filter(v -> !vehicleReservationEntityService.existsReservedByVehicleAndDateExcludingOrder(
                        v.getId(), finalTripDateForReservation, finalOrderId))
                // Exclude vehicles with expired inspection (đăng kiểm hết hạn)
                .filter(v -> v.getInspectionExpiryDate() == null || !v.getInspectionExpiryDate().isBefore(today))
                // Exclude vehicles with expired insurance (bảo hiểm hết hạn)
                .filter(v -> v.getInsuranceExpiryDate() == null || !v.getInsuranceExpiryDate().isBefore(today))
                .toList();

        // B3: Filter drivers by tripDate - 1 driver per trip per day (hard constraint)
        final LocalDate finalTripDate = tripDate;
        
        // Lấy danh sách tài xế hợp lệ cho loại xe này
        List<DriverEntity> allEligibleDrivers = driverEntityService.findByStatus(CommonStatusEnum.ACTIVE.name())
                .stream()
                .filter(d -> driverService.isCheckClassDriverLicenseForVehicleType(d, vehicleTypeEnum))
                .filter(d -> !entityService.existsActiveAssignmentForDriver(d.getId()))
                .filter(d -> !excludedDriverIds.contains(d.getId()))  // Exclude already suggested drivers
                .filter(d -> !entityService.existsAssignmentForDriverOnDate(d.getId(), finalTripDate))  // B3: Exclude drivers with assignment on tripDate
                .toList();

        // Sắp xếp xe theo mức độ sử dụng (ít dùng nhất lên đầu)
        List<UUID> vehicleIds = availableVehicles.stream().map(VehicleEntity::getId).toList();
        List<UUID> sortedVehicleIds = sortVehiclesByUsageThisMonth(vehicleIds);

        // Giới hạn số lượng xe gợi ý
        final int MAX_VEHICLES_PER_GROUP = 5;
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
                        : sizeRule.getVehicleTypeEntity().getVehicleTypeName();

                String vehicleTypeDescription = vehicle.getVehicleTypeEntity() != null
                        ? vehicle.getVehicleTypeEntity().getDescription()
                        : sizeRule.getVehicleTypeEntity().getDescription();

                vehicleSuggestions.add(new GroupedVehicleAssignmentResponse.VehicleSuggestionResponse(
                        vehicle.getId(),
                        vehicle.getLicensePlateNumber(),
                        vehicle.getModel(),
                        vehicle.getManufacturer(),
                        finalVehicleTypeId,
                        vehicleTypeName,
                        vehicleTypeDescription,
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
        final int MAX_DRIVERS_PER_VEHICLE = 6;

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

            // Factor 2: Previous assignment to this vehicle - familiarity bonus (-600 to 0 points)
            // ✅ FIXED: Familiar drivers should get BONUS (negative score = higher priority)
            int previousAssignmentIndex = lastAssignmentDriverOrder.indexOf(driverId);
            if (previousAssignmentIndex >= 0) {
                // Driver is familiar with this vehicle - BIG BONUS!
                score -= 500;  // Subtract points (lower score = higher priority)
                // Driver1 from last assignment gets extra bonus
                if (previousAssignmentIndex == 0) {
                    score -= 100;  // Primary driver gets even more bonus
                }
            }
            // Else: Driver never drove this vehicle - no bonus, no penalty (neutral)

            // Factor 3: Recent activity (weighted by our improved scoring system) (0-200 points)
            // ✅ OPTIMIZED: Reduced weight from 30 to 20 for better balance
            int recentActivity = recentActivityMap.getOrDefault(driverId, 0);
            score += recentActivity * 20;

            // Factor 4: Experience vs workload balance (0-350 points)
            // ✅ OPTIMIZED: More granular workload distribution with clear sweet spot
            int completedTrips = completedTripsMap.getOrDefault(driverId, 0);
            double workloadRatio = avgCompletedTrips > 0 ? (double) completedTrips / avgCompletedTrips : 0.0;

            if (workloadRatio < 0.3) {
                // Very inexperienced - medium-high penalty
                score += 250;
            } else if (workloadRatio < 0.7) {
                // Less experienced - medium penalty
                score += 150;
            } else if (workloadRatio < 1.3) {
                // Balanced workload - BEST! (sweet spot: 70-130% of average)
                score += 0;
            } else if (workloadRatio < 1.8) {
                // Slightly overworked - medium penalty
                score += 200;
            } else {
                // Very overworked - high penalty
                score += 350;
            }

            // Factor 5: Violations (0-300 points)
            // ✅ OPTIMIZED: Reduced weight from 80 to 60 for better balance
            int violations = violationCountMap.getOrDefault(driverId, 0);
            score += violations * 60;

            // Factor 6: Driver workload balance over time (0-400 points)
            // ✅ OPTIMIZED: More granular levels + prevents consecutive day assignments
            Optional<VehicleAssignmentEntity> lastAssignment = entityService.findLatestAssignmentByDriverId(driverId);
            if (lastAssignment.isPresent()) {
                LocalDateTime lastAssignmentDate = lastAssignment.get().getCreatedAt();
                long daysSinceLastAssignment = java.time.Duration.between(
                        lastAssignmentDate,
                        LocalDateTime.now()
                ).toDays();

                // Drivers who haven't been assigned recently get priority
                if (daysSinceLastAssignment < 1) {
                    // Just assigned yesterday - highest penalty
                    score += 400;
                } else if (daysSinceLastAssignment < 3) {
                    // Very recent (1-3 days ago)
                    score += 300;
                } else if (daysSinceLastAssignment < 7) {
                    // Recent (3-7 days ago)
                    score += 200;
                } else if (daysSinceLastAssignment < 14) {
                    // Not recent (1-2 weeks ago)
                    score += 100;
                } else if (daysSinceLastAssignment < 30) {
                    // Long rest (2-4 weeks ago)
                    score += 50;
                } else {
                    // Very long rest (>1 month) - highest priority
                    score += 0;
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
            if (segment.distanceKilometers() != null) {
                totalDistance = totalDistance.add(segment.distanceKilometers());
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
    
    /**
     * Create notifications for both customer and driver when assignment is created
     */
    private void createAssignmentNotifications(VehicleAssignmentEntity assignment, List<UUID> orderDetailIds) {
        try {
            // Get first order detail to find the order and customer
            if (orderDetailIds.isEmpty()) {
                log.warn("No order details in assignment {}", assignment.getId());
                return;
            }
            
            OrderDetailEntity firstOrderDetail = orderDetailEntityService.findEntityById(orderDetailIds.get(0))
                .orElse(null);
            if (firstOrderDetail == null || firstOrderDetail.getOrderEntity() == null) {
                log.warn("Cannot find order for assignment {}", assignment.getId());
                return;
            }
            
            OrderEntity order = firstOrderDetail.getOrderEntity();
            ContractEntity contract = contractEntityService.getContractByOrderId(order.getId()).orElse(null);
            DriverEntity driver = assignment.getDriver1();
            DriverEntity driver2 = assignment.getDriver2();
            VehicleEntity vehicle = assignment.getVehicleEntity();
            
            if (driver == null || vehicle == null) {
                log.warn("Missing driver or vehicle in assignment {}", assignment.getId());
                return;
            }
            
            // Notification 1: To Customer - DRIVER_ASSIGNED
            try {
                double totalContractValue = getEffectiveContractValue(contract);
                double depositAmount = 0.0;
                
                // Calculate deposit amount: prioritize contract's custom percent, fallback to global setting
                try {
                    BigDecimal depositPercent = getEffectiveDepositPercent(contract);
                    depositAmount = totalContractValue * depositPercent.doubleValue() / 100.0;
                    log.info("📊 Driver assigned notification - Using deposit percent: {}% (custom: {})", 
                        depositPercent, contract.getCustomDepositPercent() != null ? "yes" : "no");
                } catch (Exception e) {
                    log.warn("Could not get deposit percent, using 10% default: {}", e.getMessage());
                    depositAmount = totalContractValue * 0.1;
                }
                
                // Calculate remaining amount = total - deposit
                double remainingAmount = totalContractValue - depositAmount;
                
                // Payment deadline: now + 1 day
                LocalDateTime paymentDeadline = LocalDateTime.now().plusDays(1);
                
                // Get ALL order details for the entire order (not just this assignment)
                List<OrderDetailEntity> allOrderDetails = order.getOrderDetailEntities();
                if (allOrderDetails == null || allOrderDetails.isEmpty()) {
                    log.warn("No order details found for order {}", order.getOrderCode());
                    return;
                }
                
                // Get category description
                String categoryDescription = "Hàng hóa";
                if (order.getCategory() != null && order.getCategory().getDescription() != null) {
                    categoryDescription = order.getCategory().getDescription();
                } else if (order.getCategory() != null && order.getCategory().getCategoryName() != null) {
                    categoryDescription = order.getCategory().getCategoryName().name();
                }
                
                // Build packages metadata for ALL order details in the order
                List<Map<String, Object>> packages = new ArrayList<>();
                for (OrderDetailEntity od : allOrderDetails) {
                    Map<String, Object> pkg = new HashMap<>();
                    pkg.put("trackingCode", od.getTrackingCode());
                    pkg.put("description", od.getDescription());
                    pkg.put("weight", od.getWeightTons() + " " + od.getUnit());
                    pkg.put("weightBaseUnit", od.getWeightBaseUnit());
                    pkg.put("unit", od.getUnit());
                    packages.add(pkg);
                }
                
                // Calculate total weight from order details
                BigDecimal totalWeight = allOrderDetails.stream()
                    .map(OrderDetailEntity::getWeightTons)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                // Build metadata with full order package info
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("orderCode", order.getOrderCode());
                metadata.put("packageCount", allOrderDetails.size());
                metadata.put("totalWeight", totalWeight + " Tấn");
                metadata.put("categoryDescription", categoryDescription);
                metadata.put("packages", packages);
                metadata.put("vehicleAssignmentTrackingCode", assignment.getTrackingCode());
                metadata.put("vehiclePlate", assignment.getVehicleEntity().getLicensePlateNumber());
                metadata.put("driverName", driver.getUser().getFullName());
                metadata.put("driverPhone", driver.getUser().getPhoneNumber());
                if (driver2 != null) {
                    metadata.put("driver2Name", driver2.getUser().getFullName());
                    metadata.put("driver2Phone", driver2.getUser().getPhoneNumber());
                }
                
                // Build order detail IDs for ALL packages in the order
                List<UUID> allOrderDetailIds = allOrderDetails.stream()
                    .map(OrderDetailEntity::getId)
                    .collect(Collectors.toList());

                CreateNotificationRequest customerNotif = NotificationBuilder.buildDriverAssigned(
                    order.getSender().getUser().getId(),
                    order.getOrderCode(),
                    driver.getUser().getFullName(),
                    driver.getUser().getPhoneNumber(),
                    vehicle.getLicensePlateNumber(),
                    vehicle.getVehicleTypeEntity().getVehicleTypeName(),
                    remainingAmount,
                    paymentDeadline,
                    LocalDateTime.now().plusDays(1), // estimated pickup date
                    allOrderDetails,
                    categoryDescription,
                    assignment.getTrackingCode(),
                    order.getId(),
                    assignment.getId()
                );
                
                notificationService.createNotification(customerNotif);
                log.info("✅ Created DRIVER_ASSIGNED notification for order: {} (Total: {}, Deposit: {}, Remaining: {})", 
                    order.getOrderCode(), totalContractValue, depositAmount, remainingAmount);
                
                // Send email to drivers with their specific trip package details
                try {
                    // Get order details for THIS assignment only (for driver emails)
                    List<OrderDetailEntity> assignmentOrderDetails = orderDetailEntityService
                        .findByVehicleAssignmentId(assignment.getId());
                    if (assignmentOrderDetails == null || assignmentOrderDetails.isEmpty()) {
                        log.warn("No order details found for assignment {} to send driver emails", assignment.getId());
                    } else {
                        // Build packages metadata for THIS assignment only
                        List<Map<String, Object>> driverPackages = new ArrayList<>();
                        for (OrderDetailEntity od : assignmentOrderDetails) {
                            Map<String, Object> pkg = new HashMap<>();
                            pkg.put("trackingCode", od.getTrackingCode());
                            pkg.put("description", od.getDescription());
                            pkg.put("weight", od.getWeightTons() + " " + od.getUnit());
                            pkg.put("weightBaseUnit", od.getWeightBaseUnit());
                            pkg.put("unit", od.getUnit());
                            driverPackages.add(pkg);
                        }
                        
                        // Build metadata for driver email
                        Map<String, Object> driverMetadata = new HashMap<>();
                        driverMetadata.put("orderCode", order.getOrderCode());
                        driverMetadata.put("packageCount", assignmentOrderDetails.size());
                        
                        // Calculate total weight for this assignment
                        BigDecimal assignmentTotalWeight = assignmentOrderDetails.stream()
                            .map(OrderDetailEntity::getWeightTons)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                        
                        driverMetadata.put("totalWeight", assignmentTotalWeight + " Tấn");
                        driverMetadata.put("categoryDescription", categoryDescription);
                        driverMetadata.put("packages", driverPackages);
                        driverMetadata.put("vehicleAssignmentTrackingCode", assignment.getTrackingCode());
                        driverMetadata.put("vehiclePlate", vehicle.getLicensePlateNumber());
                        driverMetadata.put("driverName", driver.getUser().getFullName());
                        driverMetadata.put("driverPhone", driver.getUser().getPhoneNumber());
                        if (driver2 != null) {
                            driverMetadata.put("driver2Name", driver2.getUser().getFullName());
                            driverMetadata.put("driver2Phone", driver2.getUser().getPhoneNumber());
                        }
                        
                        // Send email to driver 1
                        if (driver.getUser() != null && driver.getUser().getEmail() != null) {
                            emailNotificationService.sendDriverAssignmentEmail(
                                driver.getUser().getEmail(),
                                driver.getUser().getFullName(),
                                order.getOrderCode(),
                                driverMetadata
                            );
                            log.info("✅ Sent driver assignment email to driver1: {} ({})", 
                                driver.getUser().getFullName(), driver.getUser().getEmail());
                        } else {
                            log.warn("⚠️ Driver1 {} has no email address", driver.getUser().getFullName());
                        }
                        
                        // Send email to driver 2 if exists
                        if (driver2 != null) {
                            if (driver2.getUser() != null && driver2.getUser().getEmail() != null) {
                                emailNotificationService.sendDriverAssignmentEmail(
                                    driver2.getUser().getEmail(),
                                    driver2.getUser().getFullName(),
                                    order.getOrderCode(),
                                    driverMetadata
                                );
                                log.info("✅ Sent driver assignment email to driver2: {} ({})", 
                                    driver2.getUser().getFullName(), driver2.getUser().getEmail());
                            } else {
                                log.warn("⚠️ Driver2 {} has no email address", driver2.getUser().getFullName());
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("❌ Failed to send driver assignment emails: {}", e.getMessage(), e);
                }
            } catch (Exception e) {
                log.error("❌ Failed to create DRIVER_ASSIGNED notification: {}", e.getMessage(), e);
            }
            
            // Driver notification moved to payment success handler in PayOSTransactionServiceImpl
            // NEW_ORDER_ASSIGNED will be created after customer pays full amount
            log.info("📝 Driver notification will be created after full payment for order: {}", order.getOrderCode());
            
        } catch (Exception e) {
            log.error("❌ Failed to create assignment notifications: {}", e.getMessage());
        }
    }
    
    /**
     * Get effective deposit percent for a contract.
     * Prioritizes contract's custom deposit percent if set, otherwise falls back to global setting.
     * 
     * @param contract The contract to get deposit percent for
     * @return The effective deposit percent (0-100)
     */
    private BigDecimal getEffectiveDepositPercent(ContractEntity contract) {
        // First, check if contract has custom deposit percent
        if (contract.getCustomDepositPercent() != null 
            && contract.getCustomDepositPercent().compareTo(BigDecimal.ZERO) > 0
            && contract.getCustomDepositPercent().compareTo(BigDecimal.valueOf(100)) <= 0) {
            return contract.getCustomDepositPercent();
        }
        
        // Fallback to global setting
        var contractSetting = contractSettingService.getLatestContractSetting();
        if (contractSetting != null && contractSetting.depositPercent() != null) {
            BigDecimal depositPercent = contractSetting.depositPercent();
            if (depositPercent.compareTo(BigDecimal.ZERO) > 0 && depositPercent.compareTo(BigDecimal.valueOf(100)) <= 0) {
                return depositPercent;
            }
        }
        
        // Default fallback
        log.warn("No valid deposit percent found, using 10% default");
        return BigDecimal.valueOf(10);
    }
}
