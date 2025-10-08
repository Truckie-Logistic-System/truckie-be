package capstone_project.dtos.request.vehicle;

import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.UUID;

public record VehicleFuelConsumptionCreateRequest(
    @NotNull UUID vehicleAssignmentId,
    @NotNull BigDecimal odometerReadingAtStart,
    @NotNull MultipartFile odometerAtStartImage
) {}
