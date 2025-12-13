package capstone_project.service.services.refund;

import capstone_project.dtos.request.refund.ProcessRefundRequest;
import capstone_project.dtos.response.refund.GetRefundResponse;
import capstone_project.dtos.response.refund.StaffRefundResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface RefundService {
    GetRefundResponse processRefund(ProcessRefundRequest request, MultipartFile bankTransferImage);
    GetRefundResponse getRefundByIssueId(UUID issueId);
    
    // Staff-specific methods
    List<StaffRefundResponse> getAllRefundsForStaff();
    StaffRefundResponse getRefundDetailForStaff(UUID refundId);
}
