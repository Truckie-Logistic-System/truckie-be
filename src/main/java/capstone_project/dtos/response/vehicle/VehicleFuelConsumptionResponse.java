package capstone_project.dtos.response.vehicle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record VehicleFuelConsumptionResponse(
        UUID id,
        BigDecimal fuelVolume,
        String companyInvoiceImageUrl,
        String odometerAtStartUrl,
        BigDecimal odometerReadingAtStart,
        String odometerAtEndUrl,
        BigDecimal odometerReadingAtEnd,
        BigDecimal distanceTraveled,
        LocalDateTime dateRecorded,
        String notes,
        UUID vehicleAssignmentId) { }
