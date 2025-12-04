package capstone_project.dtos.request.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateContractForCusRequest(
        String contractName,

        LocalDateTime effectiveDate,

        LocalDateTime expirationDate,

        BigDecimal adjustedValue,

        String description,

        String attachFileUrl,

        @NotBlank(message = "Order ID must not be blank")
        String orderId,

        /**
         * Trip date for vehicle reservation (when customer accepts vehicle proposal)
         */
        LocalDateTime tripDate,

        /**
         * Number of vehicles to reserve (BE will auto-select best available vehicles)
         */
        @Min(value = 0, message = "Vehicle count must be non-negative")
        Integer vehicleCount
) {
}
