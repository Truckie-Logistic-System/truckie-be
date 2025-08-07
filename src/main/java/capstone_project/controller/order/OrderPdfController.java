// File: src/main/java/capstone_project/controller/order/OrderPdfController.java
package capstone_project.controller.order;

import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.dtos.response.order.OrderPdfResponse;
import capstone_project.service.services.order.order.impl.OrderPdfService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("${pdf.api.base-path}")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class OrderPdfController {

    private final OrderPdfService orderPdfService;

    @PostMapping("/{orderId}/generate-pdf")
    public ResponseEntity<ApiResponse<OrderPdfResponse>> generateOrderPdf(@PathVariable UUID orderId) {
        OrderPdfResponse response = orderPdfService.generateAndUploadOrderPdf(orderId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}