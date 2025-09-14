package capstone_project.dtos.request.order;

import jakarta.validation.Valid;

import java.util.List;

public record CreateOrderAndDetailRequest(
        @Valid CreateOrderRequest orderRequest,
        @Valid List<CreateOrderDetailRequest> orderDetails
) {}
