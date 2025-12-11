package capstone_project.controller.refund;

import capstone_project.dtos.request.refund.ProcessRefundRequest;
import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.dtos.response.refund.GetRefundResponse;
import capstone_project.dtos.response.refund.StaffRefundResponse;
import capstone_project.service.services.refund.RefundService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${refund.api.base-path}")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class RefundController {
    private final RefundService refundService;

    @PostMapping(value = "/process", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<ApiResponse<GetRefundResponse>> processRefund(
            @RequestParam("issueId") UUID issueId,
            @RequestParam("refundAmount") BigDecimal refundAmount,
            @RequestParam("bankName") String bankName,
            @RequestParam("accountNumber") String accountNumber,
            @RequestParam("accountHolderName") String accountHolderName,
            @RequestParam("transactionCode") String transactionCode,
            @RequestParam(value = "notes", required = false) String notes,
            @RequestParam(value = "bankTransferImage", required = false) MultipartFile bankTransferImage) {

        // Validate only 1 image is uploaded
        if (bankTransferImage != null && !bankTransferImage.isEmpty()) {
            if (bankTransferImage.getSize() > 5 * 1024 * 1024) {
                throw new IllegalArgumentException("Hình ảnh phải nhỏ hơn 5MB");
            }
            if (!bankTransferImage.getContentType().startsWith("image/")) {
                throw new IllegalArgumentException("Chỉ được upload file hình ảnh");
            }
        }

        ProcessRefundRequest request = new ProcessRefundRequest(
                issueId,
                refundAmount,
                bankName,
                accountNumber,
                accountHolderName,
                transactionCode,
                notes
        );

        final var result = refundService.processRefund(request, bankTransferImage);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/issue/{issueId}")
    public ResponseEntity<ApiResponse<GetRefundResponse>> getRefundByIssueId(@PathVariable("issueId") UUID issueId) {
        final var result = refundService.getRefundByIssueId(issueId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * Get all refunds for staff management
     * Returns list sorted by newest first (createdAt DESC)
     */
    @GetMapping("/staff/list")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<StaffRefundResponse>>> getAllRefundsForStaff() {
        final var result = refundService.getAllRefundsForStaff();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * Get refund detail for staff with full information
     * Includes issue, order, vehicle assignment, and transaction details
     */
    @GetMapping("/staff/{refundId}")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<ApiResponse<StaffRefundResponse>> getRefundDetailForStaff(@PathVariable UUID refundId) {
        final var result = refundService.getRefundDetailForStaff(refundId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
