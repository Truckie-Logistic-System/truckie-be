package capstone_project.service.services.order.order.impl;

import capstone_project.dtos.response.order.OrderPdfResponse;
import capstone_project.entity.order.order.OrderEntity;
import capstone_project.service.entityServices.order.order.OrderEntityService;
import capstone_project.service.services.cloudinary.CloudinaryService;
import capstone_project.service.services.pdf.PdfGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderPdfService {

    private final OrderEntityService orderEntityService;
    private final PdfGenerationService pdfGenerationService;
    private final CloudinaryService cloudinaryService;

    public OrderPdfResponse generateAndUploadOrderPdf(UUID orderId) {
        try {
            // 1. Get the order
            OrderEntity order = orderEntityService.findById(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("Order not found with id: " + orderId));

            // 2. Generate PDF
            byte[] pdfBytes = pdfGenerationService.generateOrderPdf(order);

            // 3. Upload to Cloudinary
            String fileName = "order_" + order.getOrderCode() + "_" + System.currentTimeMillis();
            Map<String, Object> uploadResult = cloudinaryService.uploadFile(
                    pdfBytes,
                    fileName,
                    "order_pdfs"
            );

            // 4. Create and return response
            return OrderPdfResponse.builder()
                    .orderId(orderId.toString())
                    .pdfUrl((String) uploadResult.get("secure_url"))
                    .message("PDF generated and uploaded successfully")
                    .build();

        } catch (Exception e) {
            log.error("Error generating/uploading order PDF: {}", e.getMessage(), e);
            return OrderPdfResponse.builder()
                    .orderId(orderId.toString())
                    .pdfUrl(null)
                    .message("Failed to generate/upload PDF: " + e.getMessage())
                    .build();
        }
    }
}