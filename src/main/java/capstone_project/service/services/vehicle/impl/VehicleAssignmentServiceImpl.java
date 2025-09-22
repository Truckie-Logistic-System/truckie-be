package capstone_project.service.services.vehicle.impl;

import capstone_project.common.enums.CommonStatusEnum;
import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.enums.OrderStatusEnum;
import capstone_project.common.enums.VehicleTypeEnum;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.request.vehicle.UpdateVehicleAssignmentRequest;
import capstone_project.dtos.request.vehicle.VehicleAssignmentRequest;
import capstone_project.dtos.response.order.ListContractRuleAssignResult;
import capstone_project.dtos.response.order.contract.ContractRuleAssignResponse;
import capstone_project.dtos.response.user.DriverResponse;
import capstone_project.dtos.response.vehicle.SampleVehicleAssignmentResponse;
import capstone_project.dtos.response.vehicle.VehicleAssignmentResponse;
import capstone_project.dtos.response.vehicle.VehicleResponse;
import capstone_project.entity.order.contract.ContractEntity;
import capstone_project.entity.order.order.OrderEntity;
import capstone_project.entity.pricing.VehicleRuleEntity;
import capstone_project.entity.user.driver.DriverEntity;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.entity.vehicle.VehicleEntity;
import capstone_project.repository.entityServices.order.contract.ContractEntityService;
import capstone_project.repository.entityServices.order.order.OrderDetailEntityService;
import capstone_project.repository.entityServices.order.order.OrderEntityService;
import capstone_project.repository.entityServices.pricing.VehicleRuleEntityService;
import capstone_project.repository.entityServices.user.DriverEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleAssignmentEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleTypeEntityService;
import capstone_project.service.mapper.user.DriverMapper;
import capstone_project.service.mapper.vehicle.VehicleAssignmentMapper;
import capstone_project.service.mapper.vehicle.VehicleMapper;
import capstone_project.service.services.order.order.ContractRuleService;
import capstone_project.service.services.order.order.ContractService;
import capstone_project.service.services.user.DriverService;
import capstone_project.service.services.vehicle.VehicleAssignmentService;
import capstone_project.dtos.response.vehicle.SimplifiedVehicleAssignmentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private final VehicleRuleEntityService vehicleRuleEntityService;
    private final DriverService driverService;
    private final VehicleAssignmentMapper mapper;
    private final VehicleMapper vehicleMapper;
    private final DriverMapper driverMapper;

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
            UUID vehicleRuleId = response.getVehicleRuleId();
            VehicleRuleEntity vehicleRule = vehicleRuleEntityService.findEntityById(vehicleRuleId)
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
                        new SampleVehicleAssignmentResponse(response.getAssignedDetails(), detailVehicleAssignments)
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
                                        lastActiveTime          // Thời gian hoạt động gần nhất
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
}
