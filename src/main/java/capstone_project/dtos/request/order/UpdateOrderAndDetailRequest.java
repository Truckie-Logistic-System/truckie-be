package capstone_project.dtos.request.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.UUID;

import java.util.List;

public record UpdateOrderAndDetailRequest(
        @NotBlank(message = "Order ID cannot be blank")
        @UUID(message = "Order ID must be a valid UUID")
        String orderId,
        
        @Valid UpdateOrderInfoRequest orderInfo,
        @Valid List<UpdateOrderDetailInfoRequest> orderDetails
) {}
