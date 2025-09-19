package capstone_project.service.services.order.order;

import capstone_project.dtos.response.order.contract.ContractPdfResponse;
import capstone_project.dtos.response.order.contract.FullContractPDFResponse;

import java.util.UUID;

public interface OrderPdfService {
    ContractPdfResponse generateAndUploadContractPdf(UUID contractId);

    FullContractPDFResponse getFullContractPdfData(UUID contractId);
}
