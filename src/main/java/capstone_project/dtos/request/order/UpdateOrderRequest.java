package capstone_project.dtos.request.order;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.UUID;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UpdateOrderRequest (
        @NotBlank(message = "Order ID cannot be blank")
        @UUID(message = "Order ID must be a valid UUID")
        String orderId,

        @NotBlank(message = "Notes cannot be blank")
        String notes,

        @NotBlank(message = "Receiver name cannot be blank")
        String receiverName,

        @NotBlank(message = "Receiver phone cannot be blank")
        String receiverPhone,

        @NotBlank(message = "Package description cannot be blank")
        String packageDescription,


        @NotBlank(message = "Delivery address ID cannot be blank")
        @UUID(message = "Delivery address ID must be a valid UUID")
        String deliveryAddressId,

        @NotBlank(message = "Pickup address ID cannot be blank")
        @UUID(message = "Pickup address ID must be a valid UUID")
        String pickupAddressId,


        @NotBlank(message = "Category ID cannot be blank")
        @UUID(message = "Category ID must be a valid UUID")
        String categoryId
){
}
