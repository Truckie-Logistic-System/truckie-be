package capstone_project.dtos.response.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response object for vehicle fuel consumption information
 */
public record VehicleFuelConsumptionResponse(
        UUID id,
        BigDecimal odometerReadingAtStart,
        String odometerAtStartUrl,
        BigDecimal odometerReadingAtEnd,
        String odometerAtEndUrl,
        BigDecimal distanceTraveled,
        LocalDateTime dateRecorded,
        String notes,
        BigDecimal fuelVolume,
        String companyInvoiceImageUrl
) {}
