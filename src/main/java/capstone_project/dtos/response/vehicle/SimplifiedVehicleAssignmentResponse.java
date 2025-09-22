package capstone_project.dtos.response.vehicle;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO đơn giản hóa thông tin cho vehicle assignment suggestion
 * Cấu trúc mới giúp phân biệt suggestion cho từng order detail
 */
public record SimplifiedVehicleAssignmentResponse(
    Map<String, List<VehicleSuggestionDTO>> suggestionsByTrackingCode
) {
    /**
     * DTO cho xe
     */
    public record VehicleSuggestionDTO(
        UUID id,
        String licensePlateNumber,
        String model,
        String manufacturer,
        List<DriverSuggestionDTO> suggestedDrivers,
        boolean isRecommended
    ) {}

    /**
     * DTO cho tài xế với thông tin bổ sung
     */
    public record DriverSuggestionDTO(
        UUID id,
        String fullName,
        String driverLicenseNumber,
        String licenseClass,
        boolean isRecommended,
        // Thông tin bổ sung
        int violationCount,           // Số lần vi phạm
        int completedTripsCount,      // Số chuyến đã hoàn thành
        String experienceYears,       // Kinh nghiệm (năm)
        String lastActiveTime         // Thời gian hoạt động gần nhất
    ) {}
}
