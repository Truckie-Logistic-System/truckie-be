package capstone_project.dtos.response.order;

import lombok.Builder;

@Builder
public record OrderPdfResponse(
        String pdfUrl,
        String orderId,
        String message
) {}