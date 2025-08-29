package capstone_project.dtos.response.order;

import lombok.Builder;

@Builder
public record ContractPdfResponse(
        String pdfUrl,
        String contractId,
        String message
) {}