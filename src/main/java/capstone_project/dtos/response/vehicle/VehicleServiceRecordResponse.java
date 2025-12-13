package capstone_project.dtos.response.vehicle;

import java.time.LocalDateTime;
import java.util.UUID;

public record VehicleServiceRecordResponse(
        UUID id,
        // Loại và trạng thái dịch vụ
        String serviceType,
        String serviceStatus,
        // Ngày tháng
        LocalDateTime plannedDate,
        LocalDateTime actualDate,
        LocalDateTime nextServiceDate,
        // Chi tiết
        String description,
        Integer odometerReading,
        String notes,
        // Thông tin xe
        VehicleResponse vehicleEntity,
        // Metadata
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
